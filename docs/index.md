# Spring AI 2.0 项目实战

<div class="editorial-section-label">

Java 工程师的 Spring AI 2.0 重做版

</div>

## 20 周 · 18 章 + 5 个完整项目 · *0 强制外部依赖*

> 写给 Java 工程师的 Spring AI 2.0 项目实战重做版 — 跟 30 天书 v1 (spring-ai-2-0-in-action) 配套。v1 是 30 天概览版,本仓库是 20 周项目实战版,每个 chapter 配独立可跑项目 + 5 个生产级综合项目。

<div class="hero-searchbar">

<span class="hero-searchbar__hint">按 <kbd>⌘</kbd> <kbd>K</kbd> 搜索章节 / 关键词 · 或点击左上 ☰ 浏览 23 module</span>

</div>

<div class="editorial-stats">

<div class="editorial-stat">

<span class="editorial-stat__icon">🌱</span><span class="editorial-stat__num">4</span><span class="editorial-stat__label">阶段</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__icon">📖</span><span class="editorial-stat__num editorial-stat__num--accent">18</span><span class="editorial-stat__label">章</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__icon">🚀</span><span class="editorial-stat__num">5</span><span class="editorial-stat__label">项目</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__icon">📅</span><span class="editorial-stat__num editorial-stat__num--accent">20</span><span class="editorial-stat__label">周</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__icon">✈️</span><span class="editorial-stat__num">0</span><span class="editorial-stat__label">网络</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__icon">🐳</span><span class="editorial-stat__num">0</span><span class="editorial-stat__label">Docker</span>

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

## 4 阶段路线图

<div class="phase-journey">

<div class="phase-journey__card">

<span class="phase-journey__num">01</span>

<div class="phase-journey__meta">

<span class="phase-journey__tag">PHASE 1</span>

<span class="phase-journey__name">基础</span>

<span class="phase-journey__weeks">W1-4 · 6 章</span>

</div>

<ul class="phase-journey__highlights">

<li>ChatClient API</li>

<li>Streaming</li>

<li>Structured Output</li>

</ul>

<a class="phase-journey__link" href="overviews/phase-1/">Phase 1 总览 →</a>

</div>

<div class="phase-journey__arrow"><span>→</span></div>

<div class="phase-journey__card">

<span class="phase-journey__num">02</span>

<div class="phase-journey__meta">

<span class="phase-journey__tag">PHASE 2</span>

<span class="phase-journey__name">RAG</span>

<span class="phase-journey__weeks">W5-10 · 6 章</span>

</div>

<ul class="phase-journey__highlights">

<li>pgvector</li>

<li>Multi-Query</li>

<li>Re-ranking</li>

</ul>

<a class="phase-journey__link" href="overviews/phase-2/">Phase 2 总览 →</a>

</div>

<div class="phase-journey__arrow"><span>→</span></div>

<div class="phase-journey__card">

<span class="phase-journey__num">03</span>

<div class="phase-journey__meta">

<span class="phase-journey__tag">PHASE 3</span>

<span class="phase-journey__name">Agent</span>

<span class="phase-journey__weeks">W11-16 · 6 章</span>

</div>

<ul class="phase-journey__highlights">

<li>@Tool · MCP</li>

<li>Multi-Agent</li>

<li>Graph · Memory</li>

</ul>

<a class="phase-journey__link" href="overviews/phase-3/">Phase 3 总览 →</a>

</div>

<div class="phase-journey__arrow"><span>→</span></div>

<div class="phase-journey__card phase-journey__card--accent">

<span class="phase-journey__num">04</span>

<div class="phase-journey__meta">

<span class="phase-journey__tag">PHASE 4</span>

<span class="phase-journey__name">项目</span>

<span class="phase-journey__weeks">W17-20 · 5 个</span>

</div>

<ul class="phase-journey__highlights">

<li>周报 · 文档 · 旅行</li>

<li>代码审查 · 综合</li>

</ul>

<a class="phase-journey__link" href="overviews/phase-4/">Phase 4 总览 →</a>

</div>

</div>

## 23 module · 全目录

> **先看[第 0 章 · 导读](00-reading-guide.md)** — 读者画像 / 跟 v1 区别 / 20 周节奏

<div class="phase-toc">

<section class="phase-block phase-block--1">

<div class="phase-block__head">

<div class="phase-block__num">01</div>

<div class="phase-block__meta">

<div class="phase-block__kicker"><span class="phase-block__tag">PHASE 1</span><a class="phase-block__overview" href="overviews/phase-1/">总览 →</a></div>

<h3 class="phase-block__title">基础 / Spring AI 2.0 核心</h3>

<p class="phase-block__desc">6 章上手 ChatClient 全家桶 — 同步 / 流式 / Structured / Function Calling</p>

</div>

</div>

<ol class="phase-chapters">

<li class="phase-chapter"><a href="01-hello-world/"><span class="phase-chapter__num">01</span><span class="phase-chapter__body"><span class="phase-chapter__name">Hello World</span><span class="phase-chapter__desc">第一个 ChatClient,5 行代码</span></span></a></li>

<li class="phase-chapter"><a href="02-chatclient-api/"><span class="phase-chapter__num">02</span><span class="phase-chapter__body"><span class="phase-chapter__name">ChatClient API</span><span class="phase-chapter__desc">Builder / Prompt / Advisor 三件套</span></span></a></li>

<li class="phase-chapter"><a href="03-prompt-advisor/"><span class="phase-chapter__num">03</span><span class="phase-chapter__body"><span class="phase-chapter__name">Prompt + Advisor</span><span class="phase-chapter__desc">PromptTemplate + SimpleLoggerAdvisor</span></span></a></li>

<li class="phase-chapter"><a href="04-function-calling/"><span class="phase-chapter__num">04</span><span class="phase-chapter__body"><span class="phase-chapter__name">Function Calling</span><span class="phase-chapter__desc"><code>@Tool</code> / <code>@ToolParam</code></span></span></a></li>

<li class="phase-chapter"><a href="05-structured-output/"><span class="phase-chapter__num">05</span><span class="phase-chapter__body"><span class="phase-chapter__name">Structured Output</span><span class="phase-chapter__desc"><code>entity()</code> 强类型 + 自我修正</span></span></a></li>

<li class="phase-chapter"><a href="06-streaming/"><span class="phase-chapter__num">06</span><span class="phase-chapter__body"><span class="phase-chapter__name">Streaming</span><span class="phase-chapter__desc">WebFlux + SSE 实时推</span></span></a></li>

</ol>

</section>

<section class="phase-block phase-block--2">

<div class="phase-block__head">

<div class="phase-block__num">02</div>

<div class="phase-block__meta">

<div class="phase-block__kicker"><span class="phase-block__tag">PHASE 2</span><a class="phase-block__overview" href="overviews/phase-2/">总览 →</a></div>

<h3 class="phase-block__title">RAG 实战 / pgvector + 高级模式</h3>

<p class="phase-block__desc">6 章完整 RAG pipeline — Load / Split / Embed / Store / Retrieve / Production</p>

</div>

</div>

<ol class="phase-chapters">

<li class="phase-chapter"><a href="07-rag-overview/"><span class="phase-chapter__num">07</span><span class="phase-chapter__body"><span class="phase-chapter__name">RAG Overview</span><span class="phase-chapter__desc">4 步流程 + SimpleVectorStore</span></span></a></li>

<li class="phase-chapter"><a href="08-pgvector/"><span class="phase-chapter__num">08</span><span class="phase-chapter__body"><span class="phase-chapter__name">pgvector</span><span class="phase-chapter__desc">真实持久化 / HNSW / 维度配置</span></span></a></li>

<li class="phase-chapter"><a href="09-document-loaders/"><span class="phase-chapter__num">09</span><span class="phase-chapter__body"><span class="phase-chapter__name">Document Loaders</span><span class="phase-chapter__desc">MD / PDF / HTML + TokenTextSplitter</span></span></a></li>

<li class="phase-chapter"><a href="10-advanced-rag/"><span class="phase-chapter__num">10</span><span class="phase-chapter__body"><span class="phase-chapter__name">Advanced RAG</span><span class="phase-chapter__desc">Multi-Query + RetrievalAugmentationAdvisor</span></span></a></li>

<li class="phase-chapter"><a href="11-reranking/"><span class="phase-chapter__num">11</span><span class="phase-chapter__body"><span class="phase-chapter__name">Re-ranking</span><span class="phase-chapter__desc">DocumentPostProcessor + Cohere / BGE</span></span></a></li>

<li class="phase-chapter"><a href="12-rag-production/"><span class="phase-chapter__num">12</span><span class="phase-chapter__body"><span class="phase-chapter__name">RAG Production</span><span class="phase-chapter__desc">增量 / Caffeine / Actuator</span></span></a></li>

</ol>

</section>

<section class="phase-block phase-block--3">

<div class="phase-block__head">

<div class="phase-block__num">03</div>

<div class="phase-block__meta">

<div class="phase-block__kicker"><span class="phase-block__tag">PHASE 3</span><a class="phase-block__overview" href="overviews/phase-3/">总览 →</a></div>

<h3 class="phase-block__title">Agent 实战 / @Tool + MCP + Multi-Agent</h3>

<p class="phase-block__desc">6 章掌握 AI Agent — 纯 Spring AI 2.0 官方,无 LangChain4j 桥接</p>

</div>

</div>

<ol class="phase-chapters">

<li class="phase-chapter"><a href="13-agent-basics/"><span class="phase-chapter__num">13</span><span class="phase-chapter__body"><span class="phase-chapter__name">Agent Basics</span><span class="phase-chapter__desc">ChatClient + @Tool + Agent loop</span></span></a></li>

<li class="phase-chapter"><a href="14-tool-calling/"><span class="phase-chapter__num">14</span><span class="phase-chapter__body"><span class="phase-chapter__name">Tool Calling 进阶</span><span class="phase-chapter__desc">@Tool 5 特性 + FunctionToolCallback</span></span></a></li>

<li class="phase-chapter"><a href="15-mcp/"><span class="phase-chapter__num">15</span><span class="phase-chapter__body"><span class="phase-chapter__name">MCP</span><span class="phase-chapter__desc">Anthropic Model Context Protocol</span></span></a></li>

<li class="phase-chapter"><a href="16-multi-agent/"><span class="phase-chapter__num">16</span><span class="phase-chapter__body"><span class="phase-chapter__name">Multi-Agent</span><span class="phase-chapter__desc">Orchestrator-Workers 模式</span></span></a></li>

<li class="phase-chapter"><a href="17-spring-ai-graph/"><span class="phase-chapter__num">17</span><span class="phase-chapter__body"><span class="phase-chapter__name">Spring AI Graph</span><span class="phase-chapter__desc">状态机 + 条件边</span></span></a></li>

<li class="phase-chapter"><a href="18-agent-production/"><span class="phase-chapter__num">18</span><span class="phase-chapter__body"><span class="phase-chapter__name">Agent Production</span><span class="phase-chapter__desc">ChatMemory / 流式 / 安全 / 监控</span></span></a></li>

</ol>

</section>

<section class="phase-block phase-block--4">

<div class="phase-block__head">

<div class="phase-block__num">04</div>

<div class="phase-block__meta">

<div class="phase-block__kicker"><span class="phase-block__tag">PHASE 4</span><a class="phase-block__overview" href="overviews/phase-4/">总览 →</a></div>

<h3 class="phase-block__title">完整项目 / 整合实战(5 个端到端)</h3>

<p class="phase-block__desc">5 个独立可部署的 Spring Boot 项目,覆盖工程师最常见 5 类 AI 业务</p>

</div>

</div>

<ol class="phase-chapters">

<li class="phase-chapter"><a href="project-1-weekly-report/"><span class="phase-chapter__num">P1</span><span class="phase-chapter__body"><span class="phase-chapter__name">AI 周报生成器</span><span class="phase-chapter__desc">ChatClient + 0 网络 mock</span></span></a></li>

<li class="phase-chapter"><a href="project-2-doc-qa/"><span class="phase-chapter__num">P2</span><span class="phase-chapter__body"><span class="phase-chapter__name">企业文档问答</span><span class="phase-chapter__desc">关键词检索 RAG + ChatClient</span></span></a></li>

<li class="phase-chapter"><a href="project-3-travel-planner/"><span class="phase-chapter__num">P3</span><span class="phase-chapter__body"><span class="phase-chapter__name">AI 旅行规划师</span><span class="phase-chapter__desc">4 sub-agent + 1 orchestrator</span></span></a></li>

<li class="phase-chapter"><a href="project-4-code-review/"><span class="phase-chapter__num">P4</span><span class="phase-chapter__body"><span class="phase-chapter__name">AI 代码审查器</span><span class="phase-chapter__desc">@Tool 静态 + ChatClient 语义</span></span></a></li>

<li class="phase-chapter"><a href="project-5-knowledge-hub/"><span class="phase-chapter__num">P5</span><span class="phase-chapter__body"><span class="phase-chapter__name">AI 综合知识中心</span><span class="phase-chapter__desc">RAG + Tool + Memory 整合</span></span></a></li>

</ol>

</section>

</div>

------------------------------------------------------------------------

## 5 个实战项目 ✅ 2026-08-12 完成

| # | 项目 | 重点技术 | 状态 |
|---|---|---|---|
| P1 | AI 周报生成器 | ChatClient + 0 网络 mock | ✅ |
| P2 | 企业文档问答 | 关键词检索 RAG + ChatClient | ✅ |
| P3 | AI 旅行规划师 | Multi-Agent 编排 | ✅ |
| P4 | AI 代码审查器 | @Tool 静态 + ChatClient 语义 | ✅ |
| P5 | AI 综合知识中心 | QueryRouter + RAG + Tool + Memory | ✅ |

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
| 部数 | 13 章(3 phase) | **18 章(4 phase)+ 5 项目** |
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
