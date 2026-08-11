# Phase 4 · Spring AI 2.0 完整项目实战

<div class="editorial-section-label">

Spring AI 2.0 · 20 周项目实战

</div>

## 5 个端到端项目 — 把前 3 阶段能力整合成生产可跑

> 每个项目都是独立可运行的 Spring Boot 应用,**0 网络测试 PASS**。整合 Phase 1(ChatClient)+ Phase 2(RAG)+ Phase 3(Agent / Tool / Memory / Multi-Agent)。

------------------------------------------------------------------------

<div class="phase-cta">

<a href="../project-1-weekly-report.md" class="editorial-cta">▶ 开始阅读 Phase 4</a> <a href="../project-5-knowledge-hub.md" class="editorial-cta editorial-cta--ghost">↓ 直接看综合项目</a>

</div>

## TL;DR

前 3 阶段是"零件" — Phase 4 是"整车"。5 个项目覆盖 AI 工程师最常见的 5 类业务场景:

| 项目 | 业务场景 | 核心技术 |
|------|----------|----------|
| **P1** [AI 周报生成器](../project-1-weekly-report.md) | 工程师周报自动化 | ChatClient + PromptTemplate |
| **P2** [企业文档问答](../project-2-doc-qa.md) | 内部 FAQ / 制度查询 | RAG(关键词检索) + 真 LLM 路径 |
| **P3** [AI 旅行规划师](../project-3-travel-planner.md) | 用户给主题 → 完整行程 | 4 sub-agent + 1 orchestrator |
| **P4** [AI 代码审查器](../project-4-code-review.md) | PR 提交时自动 review | @Tool 静态分析 + ChatClient 语义 |
| **P5** [AI 综合知识中心](../project-5-knowledge-hub.md) | 统一 AI 入口(RAG + Tool + Memory) | QueryRouter 路由 + 多 handler |

**3 个 take-away:**
1. **完整项目骨架** — Controller / Service / DTO / application.yml / 单元测试
2. **0 网络 + 真实 LLM 双路径** — ChatClient null check, 一行切换
3. **业务可落地** — 每个项目对应一个真实可上线场景

------------------------------------------------------------------------

## 5 个项目目录

| # | 项目 | 难度 | 测试数 | 端口 |
|----|------|------|--------|------|
| P1 | [AI 周报生成器](../project-1-weekly-report.md) | ⭐ | 3 | 8080 |
| P2 | [企业文档问答](../project-2-doc-qa.md) | ⭐⭐ | 6 | 8080 |
| P3 | [AI 旅行规划师](../project-3-travel-planner.md) | ⭐⭐⭐ | 4 | 8080 |
| P4 | [AI 代码审查器](../project-4-code-review.md) | ⭐⭐⭐ | 10 | 8080 |
| P5 | [AI 综合知识中心](../project-5-knowledge-hub.md) | ⭐⭐⭐⭐ | 11 | 8080 |

<div class="editorial-stats">

<div class="editorial-stat">

<span class="editorial-stat__num">5</span><span class="editorial-stat__label">项目</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num editorial-stat__num--accent">34</span><span class="editorial-stat__label">测试</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">~30s</span><span class="editorial-stat__label">mvn test</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">0</span><span class="editorial-stat__label">网络</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">1</span><span class="editorial-stat__label">Maven repo</span>

</div>

</div>

------------------------------------------------------------------------

## 5 个核心模块(项目代码)

```
04-projects/
├── project-1-weekly-report/    # 周报自动化 (Phase 1 基础)
├── project-2-doc-qa/           # 文档问答 (Phase 2 RAG 简化)
├── project-3-travel-planner/   # 旅行规划 (Phase 3 Multi-Agent)
├── project-4-code-review/      # 代码审查 (Phase 3 @Tool 进阶)
└── project-5-knowledge-hub/    # 综合知识中心 (Phase 1+2+3 整合)
```

------------------------------------------------------------------------

## 0 网络 + 真实 LLM 双路径 Pattern

```java
@Service
public class MyService {
    private final ChatClient chatClient;

    public MyService(ChatClient.Builder builder /*, other deps */) {
        this.chatClient = (builder == null) ? null : builder.defaultSystem("...").build();
    }

    public String run(String input) {
        if (chatClient == null) {
            return mockAnswer(input);  // 0 网络可测
        }
        return chatClient.prompt().user(input).call().content();  // 真实 LLM
    }
}

// 测试 1: 0 网络
MyService svc = new MyService(null);
assertThat(svc.run("hi")).contains("mock");

// 测试 2: Spring 容器注入
@SpringBootTest
class IntegrationTest {
    @Autowired MyService svc;  // chatClient 不为 null → 真实 LLM
}
```

------------------------------------------------------------------------

## 快速开始

```bash
# 跑任一项目
cd 04-projects/project-1-weekly-report
mvn test                        # 0 网络测试 PASS
mvn spring-boot:run             # 真实跑 (需要 OPENAI_API_KEY)

# 调用
curl -X POST http://localhost:8080/report/generate \
  -H 'Content-Type: application/json' \
  -d '{"completed": ["完成 A", "修了 bug"], "planned": ["开始 B"]}'
```

------------------------------------------------------------------------

## 5 阶段 23 章总览

```
Phase 1 (ch 1-6)   Spring AI 2.0 基础 API
Phase 2 (ch 7-12)  RAG 实战
Phase 3 (ch 13-18) Agent 实战           ←  你在这
Phase 4 (ch P1-P5) 完整项目整合         ←  你在这
              ─────────────────
              23 个 module · 0 网络全测
```

------------------------------------------------------------------------

<div class="editorial-section-label">

下一步

</div>

[**P1 AI 周报生成器** →](../project-1-weekly-report.md)  ·  [Phase 3 Agent ←](phase-3.md)  ·  [回到首页 →](../index.md)
