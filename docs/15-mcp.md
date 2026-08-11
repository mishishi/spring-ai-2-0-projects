# 第 15 章 · MCP (Model Context Protocol)


## 你将学到

- ✅ MCP 协议概念: tool / resource / prompt 三件套
- ✅ spring-ai-starter-mcp-server-webmvc
- ✅ spring-ai-starter-mcp-client
- ✅ Streamable HTTP 传输
- ✅ stdio / SSE 备选

## 快速开始

```bash
cd 03-agent/15-mcp
export OPENAI_API_KEY=sk-xxxxx  # 可选, 0 网络下用 fake key 跑测试
mvn test                        # 0 网络测试
mvn spring-boot:run             # 真实跑
```

## 一句话总结

**Anthropic 协议 — Spring AI 客户端/服务端**

## 关键 API

```java
// 核心 Pattern
ChatClient agent = builder.defaultTools(toolObject).build();
String answer = agent.prompt().user(query).call().content();
```

## 核心代码

```
03-agent/15-mcp/src/main/java/
└── cc/misshi/springai/mcp/Application.java
└── cc/misshi/springai/mcp/McpDemoController.java
└── cc/misshi/springai/mcp/ProductivityTools.java
└── cc/misshi/springai/mcp/McpClientConfig.java
```

## 0 网络测试套路

```java
// 直接 new service, 传 null builder
MyService svc = new MyService(null, ...);
// service 内部 null-check, 走 mock 模板
```

## 学完下一步

读 [Phase 3 总览](overviews/phase-3.md),把 6 章串起来;或直接看 [Phase 4 项目实战](overviews/phase-4.md)。
