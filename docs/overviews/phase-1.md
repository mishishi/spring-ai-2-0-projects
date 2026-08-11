# Phase 1 · Spring AI 2.0 基础

<div class="editorial-section-label">

Spring AI 2.0 · 20 周项目实战

</div>

## 6 章上手 ChatClient 全家桶

> **30 行 Java 代码, 一个能跑的 ChatClient**。5 大 LLM Provider 切换成本 0: OpenAI / DeepSeek / Ollama / Anthropic / 通义 — 改 `application.yml` 即可。Mock LLM 模式 = 0 成本跑测试: Spring Boot Test 0 网络 0 Docker。

------------------------------------------------------------------------

<div class="phase-cta">

<a href="../01-hello-world.md" class="editorial-cta">▶ 开始阅读 Phase 1</a> <a href="../01-hello-world.md" class="editorial-cta editorial-cta--ghost">↓ 直接跳到第一章</a>

</div>

## TL;DR

**Spring AI 2.0 = Spring 官方的 AI 框架**, 跟 LangChain4j / Python LangChain 思路一致, 但更贴近 Spring 生态。**核心是 `ChatClient`**: Builder 模式 + Prompt + Advisor 三件套, 跟 `RestTemplate` 一样简单。

**3 个 take-away:**
1. **ChatClient = Builder + Prompt + Advisor + System**, 跟 RestTemplate 一个套路
2. **Spring AI 2.0 = Spring Boot 4.0+ + Java 17**, chapter 1-6 都跑通
3. **Mock LLM 模式 = 0 成本跑测试**, Spring Boot Test 0 网络 0 Docker

------------------------------------------------------------------------

## 6 章目录

| # | 章节 | 主题 | 关键 API |
|----|----|----|----|
| 01 | [Hello World](../01-hello-world.md) | 第一个 ChatClient, 5 行代码 | `ChatClient.builder().build()` |
| 02 | [ChatClient API](../02-chatclient-api.md) | Builder / Prompt / Options 深入 | `.user()` / `.system()` / `.options()` |
| 03 | [Prompt + Advisor](../03-prompt-advisor.md) | PromptTemplate + 拦截器 | `PromptTemplate` / `SimpleLoggerAdvisor` |
| 04 | [Function Calling](../04-function-calling.md) | `@Tool` + `@ToolParam` | `@Tool(description=...)` |
| 05 | [Structured Output](../05-structured-output.md) | `entity(Class)` 强类型 | `.entity(Person.class)` / `ParameterizedTypeReference` |
| 06 | [Streaming](../06-streaming.md) | WebFlux + SSE 实时推 | `.stream().content()` + `Flux<String>` |

<div class="editorial-stats">

<div class="editorial-stat">

<span class="editorial-stat__num">6</span><span class="editorial-stat__label">章</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">1</span><span class="editorial-stat__label">项目</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num editorial-stat__num--accent">~30s</span><span class="editorial-stat__label">mvn test</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">0</span><span class="editorial-stat__label">网络</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">0</span><span class="editorial-stat__label">Docker</span>

</div>

</div>

------------------------------------------------------------------------

## 5 个核心模块(项目代码)

```
01-basics/01-hello-world/src/main/java/.../
└── Application.java   # 30 行 ChatClient + CommandLineRunner

01-basics/02-chatclient-api/src/main/java/.../
└── Application.java   # 3 demo(basic / system / streaming)

01-basics/03-prompt-advisor/src/main/java/.../
└── Application.java   # PromptTemplate + SimpleLoggerAdvisor

01-basics/04-function-calling/src/main/java/.../
├── Application.java
├── TimeTools.java     # @Tool getCurrentTime / getCurrentYear
└── MathTools.java     # @Tool add / multiply

01-basics/05-structured-output/src/main/java/.../
├── Application.java
├── Person.java        # record (String name, int age, ...)
└── Movie.java         # record

01-basics/06-streaming/src/main/java/.../
├── Application.java
├── ChatController.java    # /api/chat/stream SSE
└── PageController.java    # / 单文件 HTML demo
```

------------------------------------------------------------------------

## 关键技术点(Spring AI 1.x → 2.0 升级)

| 维度 | 1.x | **2.0**(本仓库) |
|----|----|----|
| Spring Boot | 3.4.x | **4.0.0** |
| Spring AI | 1.1.3 | **2.0.0 GA** |
| `Document.getContent()` | ✅ | **`getText()`** |
| `QuestionAnswerAdvisor` 包 | `qa` | **`vectorstore`** |
| Builder 命名 | `withDimensions` | **`dimensions`(去 with 前缀)** |
| 工具调用 | `chatModel.call()` | **必须 `ChatClient` + `ToolCallingAdvisor`** |
| 测试 | `WebEnvironment.MOCK` + `jakarta.websocket` | **`WebEnvironment.NONE` 避开 web** |

完整升级指南见 [Chapter 1 README](https://github.com/mishishi/spring-ai-2-0-projects/blob/main/01-basics/01-hello-world/README.md)。

------------------------------------------------------------------------

## 快速开始

```bash
cd 01-basics/01-hello-world
mvn test                          # 1 test, ~10s, 0 网络
mvn spring-boot:run               # 端口 8080
```

------------------------------------------------------------------------

<div class="editorial-section-label">

下一步

</div>

[**01 Hello World** →](../01-hello-world.md)  ·  [Phase 2 RAG →](phase-2.md)
