# Project 5: AI 综合知识中心

> Phase 4 实战项目 5/5 · 整合 RAG + @Tool + ChatMemory + Multi-Agent

## 解决什么问题

前面 4 个项目是分散的 AI 能力 — RAG 答文档、Tool 审代码、Memory 记对话、Multi-Agent 协作。
真实业务需要一个 **统一入口**:用户问任意问题,系统自动判断走哪个处理路径。
这就是 Knowledge Hub — 一个 **意图路由 + 多 handler** 的 AI 平台雏形。

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

| 组件 | 角色 | 简化实现 |
|------|------|----------|
| `QueryRouter` | 意图分类 | 关键词规则匹配 |
| `KnowledgeBase` | RAG | 中文 2-gram 关键词检索(10 条预置) |
| `CodeSnippetAnalyzer` | @Tool | 反模式 / 圈复杂度 |
| `ConversationMemory` | 多轮记忆 | ArrayDeque + ConcurrentHashMap |
| `KnowledgeHubService` | 协调器 | 路由 → handler → 记忆 |

## 跑起来

```bash
# 0 网络测试(5 个 router + 5 个 service + 1 个 context)
mvn -pl 04-projects/project-5-knowledge-hub test

# 真实跑
export OPENAI_API_KEY=sk-xxxxx
cd 04-projects/project-5-knowledge-hub
mvn spring-boot:run

# 调用
curl -X POST http://localhost:8080/hub/ask \
  -H 'Content-Type: application/json' \
  -d '{"sessionId": "alice", "query": "什么是 RAG?"}'

# 代码审查
curl -X POST http://localhost:8080/hub/ask \
  -H 'Content-Type: application/json' \
  -d '{"sessionId": "alice", "query": "请审查代码", "codeSnippet": "public void m() { System.out.println(\"hi\"); }"}'

# 周报生成
curl -X POST http://localhost:8080/hub/ask \
  -H 'Content-Type: application/json' \
  -d '{"sessionId": "alice", "query": "生成本周周报", "bulletPoints": ["完成 A", "修复 B"]}'
```

## 核心代码

- `KnowledgeHubService` — 路由 + dispatch + 记忆
- `QueryRouter` — 关键词规则匹配
- `KnowledgeBase` — RAG 简化版
- `CodeSnippetAnalyzer` — 静态分析
- `ConversationMemory` — 多轮记忆
- `KnowledgeHubController` — POST /hub/ask

## 学到啥

- Spring AI 2.0 综合应用(整合 1-18 章能力)
- 意图路由(QueryRouter)的设计
- 多轮记忆的实现(MessageWindowChatMemory 简化版)
- RAG + Tool + Memory + Multi-Agent 协作模式
- "AI 平台"的项目结构雏形

## 扩展方向

- 真实 LLM 时,QueryRouter 用 ChatClient 做意图分类
- 换 SimpleVectorStore + EmbeddingModel 替 KnowledgeBase
- 接 MCP Server(章节 15)接入外部工具
- 加 WebSocket 流式输出
- 加 Spring Security 鉴权
- 部署到云(Aliyun / AWS)
