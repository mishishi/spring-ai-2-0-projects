# P5 · AI 综合知识中心


## 一句话总结

**RAG + Tool + Memory + Multi-Agent 一体化** — 统一 AI 入口,QueryRouter 根据 query 类型自动路由到 DocQA / CodeReview / WeeklyReport / Chitchat 4 个 handler,5 大组件协作。

## 你将学到

- ✅ AI 平台项目结构(组件解耦 + 路由 + 记忆)
- ✅ QueryRouter 关键词意图分类
- ✅ ConversationMemory 多轮对话(简化版 MessageWindowChatMemory)
- ✅ 整合前 4 个项目的能力
- ✅ Spring Boot 5 个 component 协作模式

## 快速开始

```bash
cd 04-projects/project-5-knowledge-hub
mvn test                          # 0 网络 11 tests, ~7s
mvn spring-boot:run

# 文档问答
curl -X POST http://localhost:8080/hub/ask \
  -H 'Content-Type: application/json' \
  -d '{"sessionId": "alice", "query": "RAG 的全称是什么?"}'

# 代码审查
curl -X POST http://localhost:8080/hub/ask \
  -H 'Content-Type: application/json' \
  -d '{"sessionId": "alice", "query": "请审查代码", "codeSnippet": "public void m() { System.out.println(\"hi\"); }"}'

# 周报生成
curl -X POST http://localhost:8080/hub/ask \
  -H 'Content-Type: application/json' \
  -d '{"sessionId": "alice", "query": "生成本周周报", "bulletPoints": ["完成 A", "修复 B"]}'
```

## 架构

```
  POST /hub/ask
  { sessionId, query, codeSnippet?, bulletPoints? }
        │
        ▼
  ┌──────────────────────────────────────┐
  │ QueryRouter.route(query)             │
  │   - "代码 / 审查"    → CODE_REVIEW   │
  │   - "周报 / 总结"    → WEEKLY_REPORT │
  │   - "什么是 / 怎么"  → DOC_QA        │
  │   - "你好 / hi"      → CHITCHAT      │
  │   - 其他             → DOC_QA (默认) │
  └────────┬─────────────────────────────┘
           │
           ▼
  ┌──────────────────────────────────────┐
  │ handler dispatch                     │
  │   DOC_QA       → KnowledgeBase.search│
  │   CODE_REVIEW  → CodeSnippetAnalyzer │
  │   WEEKLY_REPORT → 模板渲染            │
  │   CHITCHAT     → 静态回复            │
  └────────┬─────────────────────────────┘
           │
           ▼
  ┌──────────────────────────────────────┐
  │ ConversationMemory 记录上下文         │
  │ 每个 sessionId 保留最近 20 条消息      │
  └──────────────────────────────────────┘
```

## 5 大组件

| 组件 | 角色 | 简化实现 | 真实 LLM 替代 |
|------|------|----------|----------------|
| `QueryRouter` | 意图分类 | 关键词规则匹配 | ChatClient LLM 分类 |
| `KnowledgeBase` | RAG | 中文 2-gram 关键词检索 | SimpleVectorStore + Embedding |
| `CodeSnippetAnalyzer` | @Tool | 7 大反模式 + 圈复杂度 | 加更多 rule + AST |
| `ConversationMemory` | 多轮记忆 | ArrayDeque + ConcurrentHashMap | MessageWindowChatMemory |
| `KnowledgeHubService` | 协调器 | 路由 → handler → 记忆 | 真实 LLM 流式 |

## 关键代码

### 1. QueryRouter 路由

```java
@Component
public class QueryRouter {
    public enum Route { DOC_QA, CODE_REVIEW, WEEKLY_REPORT, CHITCHAT, UNKNOWN }

    public Route route(String query) {
        if (query == null || query.isBlank()) return Route.UNKNOWN;
        String lower = query.toLowerCase(Locale.ROOT);

        if (containsAny(lower, List.of("代码", "code", "review", "审查", "bug", "重构"))) {
            return Route.CODE_REVIEW;
        }
        if (containsAny(lower, List.of("周报", "weekly", "总结", "summary", "本周"))) {
            return Route.WEEKLY_REPORT;
        }
        if (containsAny(lower, List.of("怎么", "如何", "什么", "是什么", "?"))) {
            return Route.DOC_QA;
        }
        if (containsAny(lower, List.of("你好", "hi", "hello"))) {
            return Route.CHITCHAT;
        }
        return Route.DOC_QA;  // fallback
    }
}
```

**0 网络** 关键词规则,**真实 LLM** 用 ChatClient 做意图分类(更准但需要 token)。

### 2. KnowledgeBase 关键词检索

```java
@Component
public class KnowledgeBase {
    private final List<Doc> docs = new ArrayList<>();

    public KnowledgeBase() {
        add("公司年假制度: 入职 1 年 5 天,3 年 10 天...");
        add("Spring AI 是什么: Spring 团队推出的 AI 集成框架...");
        add("RAG 全称: Retrieval-Augmented Generation...");
        add("MCP 是什么: Model Context Protocol...");
        // 共 10 条预置知识
    }

    public List<Doc> search(String query, int topK) {
        // 跟 P2 doc-qa 一样的中文 2-gram 评分
        ...
    }

    public record Doc(String id, String text) {}
}
```

预置 10 条知识,涵盖公司制度 + Spring AI 概念。

### 3. CodeSnippetAnalyzer

```java
@Component
public class CodeSnippetAnalyzer {
    public CodeReviewSummary review(String code) {
        int totalLines = code.split("\n").length;
        int complexity = countKeywords(code, new String[]{"if", "while", "for", "&&", "||"}) + 1;

        List<String> issues = new ArrayList<>();
        if (code.contains("System.out.println")) issues.add("使用 System.out.println");
        if (code.contains("password")) issues.add("疑似硬编码密码");
        if (code.contains("catch (") && code.contains(") { }")) issues.add("空 catch 块");
        if (code.contains("TODO") || code.contains("FIXME")) issues.add("存在 TODO / FIXME");
        // ...

        return new CodeReviewSummary(totalLines, complexity, issues);
    }
}
```

复用 P4 项目的核心逻辑,简化版。

### 4. ConversationMemory 多轮记忆

```java
@Component
public class ConversationMemory {
    public static final int MAX_MESSAGES = 20;
    private final Map<String, Deque<Entry>> store = new ConcurrentHashMap<>();

    public void add(String sessionId, String role, String content) {
        Deque<Entry> q = store.computeIfAbsent(sessionId, k -> new ArrayDeque<>(MAX_MESSAGES));
        if (q.size() >= MAX_MESSAGES) q.pollFirst();  // 滑动窗口
        q.addLast(new Entry(role, content));
    }

    public List<Entry> recent(String sessionId) {
        Deque<Entry> q = store.get(sessionId);
        return q == null ? List.of() : new ArrayList<>(q);
    }

    public record Entry(String role, String content) {}
}
```

**简化版** MessageWindowChatMemory(章节 18)。真实生产换 Spring AI 官方实现或 Redis 持久化。

### 5. KnowledgeHubService 协调

```java
@Service
public class KnowledgeHubService {
    public HubResponse handle(HubRequest req) {
        String sessionId = req.sessionId() == null ? "default" : req.sessionId();
        String query = req.query() == null ? "" : req.query();

        // 1. 记忆
        memory.add(sessionId, "user", query);

        // 2. 路由
        QueryRouter.Route route = router.route(query);

        // 3. 处理
        String answer = switch (route) {
            case DOC_QA -> handleDocQa(query);
            case CODE_REVIEW -> handleCodeReview(query, req.codeSnippet());
            case WEEKLY_REPORT -> handleWeeklyReport(query, req.bulletPoints());
            case CHITCHAT -> handleChitchat(query);
            case UNKNOWN -> "我没有理解...";
        };

        // 4. 记忆
        memory.add(sessionId, "assistant", answer);

        // 5. 返回(含历史)
        return new HubResponse(route.name(), answer, memory.recent(sessionId), knowledgeBase.size());
    }
}
```

**核心模式**:
- 路由 → dispatch → 记忆 → 返回
- 每次 handle 自动维护 sessionId 的历史
- 0 网络下,所有 handler 都是 mock

## 3 个 Demo

### Demo 1: 文档问答

```bash
curl -X POST http://localhost:8080/hub/ask -d '{
  "sessionId": "alice",
  "query": "RAG 的全称是什么?"
}'
```

返回:
```json
{
  "route": "DOC_QA",
  "answer": "【知识库回答】\n> RAG 全称: Retrieval-Augmented Generation...",
  "history": [...],
  "knowledgeBaseSize": 10
}
```

### Demo 2: 代码审查

```bash
curl -X POST http://localhost:8080/hub/ask -d '{
  "sessionId": "alice",
  "query": "请审查代码",
  "codeSnippet": "public void m() { System.out.println(\"hi\"); }"
}'
```

返回 CODE_REVIEW 路由,answer 包含"使用 System.out.println"。

### Demo 3: 多轮记忆

```bash
# alice 第一次
curl -X POST .../hub/ask -d '{"sessionId":"alice","query":"什么是 RAG?"}'
# alice 第二次(同 session)
curl -X POST .../hub/ask -d '{"sessionId":"alice","query":"那 MCP 呢?"}'
```

第二次 response 的 `history` 包含第一次的 user + assistant(共 4 条:2 user + 2 assistant)。

## 踩坑(3 大常见)

### 坑 1: 中文检索大小写

```java
// ❌ query "RAG" 搜不到 doc 里的 "rag"
String lower = text.toLowerCase();
lower.indexOf("RAG");  // 大小写敏感!

// ✅ 统一 lowercase 比较
String tLower = t.toLowerCase();
lower.indexOf(tLower);
```

### 坑 2: SessionId 缺失导致共享对话

```java
// ❌ null sessionId → 所有人共享"default"
memory.add("default", "user", query);

// ✅ 用用户 ID 作 sessionId(从 JWT / cookie 拿)
String sessionId = req.sessionId() == null ? "default" : req.sessionId();
```

### 坑 3: 记忆无限增长

```java
// ❌ ConcurrentHashMap 永远增长
public void add(...) { store.get(sessionId).add(...); }

// ✅ 滑动窗口,超过 MAX 弹最旧
if (q.size() >= MAX_MESSAGES) q.pollFirst();
q.addLast(...);
```

## 0 网络测试

11 tests:
- QueryRouter 5 种路由各 1 test
- KnowledgeBase 搜索 top-K
- CodeSnippetAnalyzer 反模式
- ConversationMemory 多 session 隔离
- HubService 完整 5 个 handler dispatch
- 默认参数(empty / null)

## 实战清单

- [x] 5 组件协作(QueryRouter / KnowledgeBase / CodeAnalyzer / Memory / HubService)
- [x] 4 种路由
- [x] 多 session 记忆隔离
- [x] 0 网络 + 真实 LLM 双路径
- [ ] **生产补 1**:QueryRouter 用 LLM 意图分类
- [ ] **生产补 2**:KnowledgeBase 换 SimpleVectorStore + Embedding
- [ ] **生产补 3**:Memory 换 MessageWindowChatMemory(章节 18) + Redis 持久化
- [ ] **生产补 4**:接 MCP Server(章节 15)暴露给 Claude Desktop

## 完整代码

[04-projects/project-5-knowledge-hub/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/04-projects/project-5-knowledge-hub)

## 🎉 Phase 4 (完整项目实战) 完结

5 个端到端项目全完成!

| 项目 | 整合能力 | 状态 |
|------|----------|------|
| P1 周报生成 | Phase 1 ChatClient 基础 | ✅ |
| P2 文档问答 | Phase 2 RAG(简化关键词版) | ✅ |
| P3 旅行规划 | Phase 3 Multi-Agent | ✅ |
| P4 代码审查 | Phase 3 @Tool 进阶 | ✅ |
| P5 综合知识 | Phase 1+2+3 整合 | ✅ |

**23 module 全部完成**:Phase 1 (6) + Phase 2 (6) + Phase 3 (6) + Phase 4 (5)

## 学完下一步

想看更深的?回 [Phase 3 进阶 →](18-agent-production.md) 看生产化细节,或 [Phase 2 完整 RAG →](12-rag-production.md) 看 pgvector 真持久化。
