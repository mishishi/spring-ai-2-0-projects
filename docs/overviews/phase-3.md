# Phase 3 · Spring AI 2.0 Agent 实战

<div class="editorial-section-label">

Spring AI 2.0 · 20 周项目实战

</div>

## 6 章搞定 AI Agent — 从单 Tool 到多 Agent 到 Graph

> **纯 Spring AI 2.0 官方**,无 LangChain4j 桥接。@Tool / MCP / ChatMemory / MessageWindow / Graph 一站式。0 网络可测,ChatClient 真实 LLM 路径一行切换。

------------------------------------------------------------------------

<div class="phase-cta">

<a href="../13-agent-basics.md" class="editorial-cta">▶ 开始阅读 Phase 3</a> <a href="../14-tool-calling.md" class="editorial-cta editorial-cta--ghost">↓ 直接看 @Tool 进阶</a>

</div>

## TL;DR

**Agent = LLM + 工具 + 记忆 + 决策循环**。Spring AI 2.0 把 LangChain/LlamaIndex 那套"提示工程 + tool 调用 + 多 agent"的能力用 Spring 风格原生化 — 你不用离开 Spring Boot 就能搭生产级 Agent。

**3 个 take-away:**
1. **@Tool 注解** — Java 方法直接当 LLM 工具,Spring AI 自动注入 schema
2. **MCP (Model Context Protocol)** — Anthropic 标准的"tool 互操作协议",多模型/多语言通用
3. **Multi-Agent** — Orchestrator-Workers 模式,4 个 ChatClient Bean 协作

------------------------------------------------------------------------

## 6 章目录

| # | 章节 | 主题 | 关键 API |
|----|----|----|----|
| 13 | [Agent Basics](../13-agent-basics.md) | ChatClient + @Tool 起步 | `.defaultTools(tools)` |
| 14 | [Tool Calling 进阶](../14-tool-calling.md) | @Tool 5 大特性 | `returnDirect` / `required` / `ToolContext` / POJO / `FunctionToolCallback` |
| 15 | [MCP](../15-mcp.md) | Anthropic Model Context Protocol | `spring-ai-starter-mcp-server-webmvc` + `spring-ai-starter-mcp-client` |
| 16 | [Multi-Agent](../16-multi-agent.md) | Orchestrator-Workers | 4 ChatClient Bean + 1 编排 service |
| 17 | [Spring AI Graph](../17-spring-ai-graph.md) | 状态机 + 条件边 | `addNode` / `addEdge` / `addConditionalEdge` |
| 18 | [Agent Production](../18-agent-production.md) | 记忆 / 流式 / 安全 / 监控 | `MessageWindowChatMemory` / `@Tool(safeMath)` / Actuator |

<div class="editorial-stats">

<div class="editorial-stat">

<span class="editorial-stat__num">6</span><span class="editorial-stat__label">章</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">~30s</span><span class="editorial-stat__label">mvn test</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">0</span><span class="editorial-stat__label">网络</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num editorial-stat__num--accent">1</span><span class="editorial-stat__label">MCP 协议</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">4</span><span class="editorial-stat__label">sub-agent</span>

</div>

</div>

------------------------------------------------------------------------

## 6 个核心模块(项目代码)

```
03-agent/13-agent-basics/src/main/java/.../    # ChatClient + @Tool + 简单 agent loop
03-agent/14-tool-calling/src/main/java/.../    # @Tool 5 特性
03-agent/15-mcp/src/main/java/.../             # MCP Client + Server (Streamable HTTP)
03-agent/16-multi-agent/src/main/java/.../     # 4 ChatClient Bean 编排
03-agent/17-spring-ai-graph/src/main/java/.../ # Graph 状态机
03-agent/18-agent-production/src/main/java/.../# ChatMemory + Streaming + 安全 + 监控
```

------------------------------------------------------------------------

## Agent 技术栈全景

```
                    ┌─────────────────────────────────┐
                    │        User Query              │
                    └────────────┬────────────────────┘
                                 │
                                 ▼
            ┌────────────────────────────────────────┐
            │  ChatClient + defaultTools(tools)       │
            │  + defaultAdvisors(memoryAdvisor)       │
            └────────────┬───────────────────────────┘
                         │
            ┌────────────┼────────────┐
            ▼            ▼            ▼
      ┌──────────┐ ┌──────────┐ ┌──────────┐
      │  @Tool   │ │   MCP    │ │ ChatMem  │
      │ Method   │ │ Server   │ │ Message  │
      │ (Java)   │ │ (Stream) │ │ Window   │
      └────┬─────┘ └────┬─────┘ └────┬─────┘
           │            │            │
           ▼            ▼            ▼
       [execute]    [remote tool] [context]
```

------------------------------------------------------------------------

## 0 网络测试套路

```java
// Pattern 1: null builder → service 内部 mock
MyService svc = new MyService(null, deps...);
svc.run();  // 走 mock 路径

// Pattern 2: 直接 new tool, 调静态方法
WeatherTools wt = new WeatherTools();
String result = wt.getCurrentWeather("北京");
assertThat(result).contains("晴");
```

每个 chapter 都用这套 — `mvn test` 全绿,不依赖 OPENAI_API_KEY。

------------------------------------------------------------------------

## 快速开始

```bash
# 跑 Phase 3 第一个 chapter
cd 03-agent/13-agent-basics
mvn test
mvn spring-boot:run   # (需要 OPENAI_API_KEY)
```

------------------------------------------------------------------------

<div class="editorial-section-label">

下一步

</div>

[**13 Agent Basics** →](../13-agent-basics.md)  ·  [Phase 2 RAG ←](phase-2.md)  ·  [Phase 4 完整项目 →](phase-4.md)
