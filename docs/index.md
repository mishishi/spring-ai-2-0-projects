# Spring AI 2.0 项目实战

<div class="editorial-section-label">

Java 工程师的 Spring AI 2.0 重做版

</div>

## 20 周 · 12 章 + 5 个完整项目 · *0 强制外部依赖*

> 写给 Java 工程师的 Spring AI 2.0 项目实战重做版 — 跟 30 天书 v1 (spring-ai-2-0-in-action) 配套。v1 是 30 天概览版,本仓库是 20 周项目实战版,每个 chapter 配独立可跑项目 + 5 个生产级综合项目。

<div class="editorial-stats">

<div class="editorial-stat">

<span class="editorial-stat__num">2</span><span class="editorial-stat__label">阶段</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num editorial-stat__num--accent">12</span><span class="editorial-stat__label">章</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">5</span><span class="editorial-stat__label">项目</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num editorial-stat__num--accent">20</span><span class="editorial-stat__label">周</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">0</span><span class="editorial-stat__label">网络</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">0</span><span class="editorial-stat__label">Docker</span>

</div>

</div>

------------------------------------------------------------------------

## TL;DR

**Spring AI 2.0 = Spring 官方的 AI 框架**, 跟 LangChain4j / Python LangChain 思路一致, 但更贴近 Spring 生态。**30 行 Java 代码就能跑起一个 ChatClient, 0 强制外部依赖** (Mock LLM 模式可离线跑测试)。

**3 个 take-away:**
1. **Spring AI 2.0 核心是 `ChatClient`** — Builder + Prompt + Advisor + Function Calling + RAG + Agent
2. **完整 RAG 实战** — Load / Split / Embed / Store / Retrieve,pgvector 真实持久化
3. **生产级三件套** — 增量更新 / Caffeine 缓存 / Actuator 监控

------------------------------------------------------------------------

## 2 阶段路线图

```
+--------------------------------------------------------------------+
|  Spring AI 2.0 项目实战  ·  20 周                                |
+--------------------------------------------------------------------+
|  Phase 1 (基础)  →  Phase 2 (RAG 实战)                           |
|  6 章 + 1 项目       6 章 + 4 项目                                |
|  Hello World →       RAG 完整 pipeline                           |
|  Streaming           pgvector / Multi-Query / Re-ranking         |
+--------------------------------------------------------------------+
|  ✅ 12 chapter / 12 module / mvn test 43.5s / 0 网络 0 Docker  |
+--------------------------------------------------------------------+
```

## 12 章 · 全目录

> **先看[第 0 章 · 导读](00-reading-guide.md)** — 读者画像 / 跟 v1 区别 / 20 周节奏

<div class="toc-grid">

<div class="toc-phase">

### <span class="toc-phase-label toc-phase-1">PHASE 1</span> [基础 (Spring AI 2.0 核心)](overviews/phase-1.md)

6 章上手 ChatClient 全家桶 — 同步 / 流式 / Structured / Function Calling

- **[01 Hello World](01-hello-world.md)** — 第一个 ChatClient, 5 行代码
- **[02 ChatClient API](02-chatclient-api.md)** — Builder / Prompt / Advisor 三件套
- **[03 Prompt + Advisor](03-prompt-advisor.md)** — PromptTemplate + SimpleLoggerAdvisor
- **[04 Function Calling](04-function-calling.md)** — `@Tool` / `@ToolParam`
- **[05 Structured Output](05-structured-output.md)** — `entity()` 强类型
- **[06 Streaming](06-streaming.md)** — WebFlux + SSE 实时推

</div>

<div class="toc-phase">

### <span class="toc-phase-label toc-phase-2">PHASE 2</span> [RAG 实战 (pgvector + 高级模式)](overviews/phase-2.md)

6 章完整 RAG pipeline — Load / Split / Embed / Store / Retrieve / Production

- **[07 RAG Overview](07-rag-overview.md)** — 4 步流程 + SimpleVectorStore
- **[08 pgvector](08-pgvector.md)** — 真实持久化 / HNSW / 维度配置
- **[09 Document Loaders](09-document-loaders.md)** — MD / PDF / HTML + TokenTextSplitter
- **[10 Advanced RAG](10-advanced-rag.md)** — Multi-Query + RetrievalAugmentationAdvisor
- **[11 Re-ranking](11-reranking.md)** — DocumentPostProcessor + Cohere / BGE
- **[12 RAG Production](12-rag-production.md)** — 增量 / Caffeine / Actuator

</div>

</div>

------------------------------------------------------------------------

## 5 个实战项目(待开始)

| # | 项目 | 重点技术 | 状态 |
|---|---|---|---|
| 1 | AI 周报生成器 | PromptTemplate + Structured Output | ⏳ W17 |
| 2 | 企业文档问答助手 | DocumentReader + pgvector + Re-rank | ⏳ W18 |
| 3 | AI 旅行规划助手 | Multi-Agent + MCP + Memory | ⏳ W19 |
| 4 | AI Code Review 助手 | RAG + Agent + MCP | ⏳ W20 |
| 5 | 企业知识中心 | 4 个项目整合 + Web UI | ⏳ 拓展 |

------------------------------------------------------------------------

## 0 强制外部依赖

跟 30 天书风格一致 — **测试永远不连真实 LLM**:

<div class="editorial-card">

<div class="editorial-card__title">

🧠 LLM 调用

</div>

<div class="editorial-card__body">

真实生产: OpenAI / DeepSeek / Ollama / 通义

</div>

<div class="editorial-card__fix">

**本仓库默认**: `application.yml` 用 `${OPENAI_API_KEY:fake-key-for-tests}` 默认值,`@Profile("!test")` 屏蔽 CommandLineRunner,0 网络

</div>

</div>

<div class="editorial-card">

<div class="editorial-card__title">

📐 Embedding

</div>

<div class="editorial-card__body">

真实生产: OpenAI text-embedding-3-small

</div>

<div class="editorial-card__fix">

**本仓库默认**: VectorStore 延迟初始化,contextLoads 不调 embedding

</div>

</div>

<div class="editorial-card">

<div class="editorial-card__title">

🗄️ Vector Store

</div>

<div class="editorial-card__body">

真实生产: pgvector(Docker 5433 端口)

</div>

<div class="editorial-card__fix">

**本仓库默认**: Spring context 启动时连接 pgvector,add/query 时才调 OpenAI

</div>

</div>

------------------------------------------------------------------------

## 快速开始 (3 行命令)

```bash
git clone https://github.com/mishishi/spring-ai-2-0-projects.git
cd spring-ai-2-0-projects

# Phase 1 第一个 chapter
cd 01-basics/01-hello-world
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

------------------------------------------------------------------------

## 跟 30 天书 v1 的关系

| 维度 | v1 [30 天书](https://spring-ai-2-0-in-action.vercel.app) | **v2 项目实战(本仓库)** |
|---|---|---|
| 定位 | 概览 / breadth | **项目实战 / depth** |
| 部数 | 13 章(3 phase) | **12 章(2 phase)+ 5 项目** |
| 实战项目 | 3 个 | **5 个完整可部署** |
| 风格 | The Verge 杂志风 | **The Verge 杂志风(同款)** |
| 状态 | 归档(2026-08-11) | **进行中** |

**互补关系**: 30 天书 v1 讲 breadth,v2 讲 depth + 项目实战。

------------------------------------------------------------------------

## 反馈 / 贡献

- **GitHub**: [mishishi/spring-ai-2-0-projects](https://github.com/mishishi/spring-ai-2-0-projects)
- **问题反馈**: GitHub Issues
- **配套 v1**: [spring-ai-2-0-in-action](https://spring-ai-2-0-in-action.vercel.app)

**License**: MIT

© 2026 Jason · Spring AI 2.0 项目实战
