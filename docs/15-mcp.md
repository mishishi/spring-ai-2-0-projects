# 第 15 章 · MCP (Model Context Protocol)


## 一句话总结

**MCP = Anthropic 标准的"AI 工具互操作协议"** — Spring AI 2.0 内置 Server + Client,让任何支持 MCP 的客户端(Claude Desktop / 其他 AI 应用)都能发现并调用你的 Java 工具。

## 你将学到

- ✅ MCP 协议核心:tool / resource / prompt 三件套
- ✅ `spring-ai-starter-mcp-server-webmvc` 把 @Tool 暴露成 MCP 端点
- ✅ `spring-ai-starter-mcp-client` 连接外部 MCP server
- ✅ Streamable HTTP transport(替代旧 SSE)
- ✅ 同一个 Spring Boot 应用既是 Server 又是 Client

## 快速开始

```bash
cd 03-agent/15-mcp
mvn test                          # 0 网络 8 tests
mvn spring-boot:run

# Server 端: @Tool 自动注册到 MCP 端点 (默认 /mcp/messages)
# Client 端: 自动连接本进程的 server,LLM 可以调工具
curl "http://localhost:8080/mcp/demo/ask?question=创建任务:写周报"
```

## 关键概念:MCP 是什么

```
┌──────────────────────────────────────┐
│  MCP Host (Claude Desktop / 你的 app) │
│  ↓                                  │
│  MCP Client (我们的 Spring AI)        │
│  ↓ JSON-RPC over Streamable HTTP   │
│  MCP Server (Spring AI 暴露的)        │
│  ↓                                  │
│  @Tool 方法 (Java)                   │
└──────────────────────────────────────┘
```

**MCP 跟 @Tool 的关系**:
- `@Tool` = 在自己进程内调工具
- MCP = 把工具通过标准协议暴露给**别的进程/别的应用**

Anthropic 在 2024-11 发布 MCP,2025 年被 OpenAI / Google / Cursor 等全部采用,成为"AI 工具 HTTP"事实标准。

## 关键 API

### 1. MCP Server 端(pom 依赖)

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

application.yml:
```yaml
spring:
  ai:
    mcp:
      server:
        name: productivity-mcp
        version: 1.0.0
        type: SYNC     # 同步 server
        sse-message-endpoint: /mcp/messages
```

`@Tool` 注解的方法自动注册到 MCP server 端点:
```java
@Component
public class ProductivityTools {
    @Tool(description = "创建一条新任务,返回任务 ID")
    public String createTask(
            @ToolParam(description = "任务标题") String title,
            @ToolParam(description = "任务描述") String description) {
        long id = Math.abs((title + description).hashCode() % 100000);
        return String.format("任务已创建: ID=%d, 标题='%s'", id, title);
    }
}
```

### 2. MCP Client 端(pom 依赖)

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

application.yml:
```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        stdio:    # stdio 模式连接外部 server
          servers:
            my-server:
              command: java
              args: ["-jar", "/path/to/server.jar"]
        # 或 streamable-http 连接
        streamable-http:
          connections:
            remote:
              url: http://localhost:8081
```

### 3. Client ChatClient 装配

```java
@Bean
public ChatClient mcpChatClient(ChatClient.Builder builder, ToolCallbackProvider mcpTools) {
    return builder
            .defaultSystem("你是办公助手,所有操作都通过 MCP 工具完成。")
            .defaultTools(mcpTools)  // 关键:从 MCP client 拿工具
            .build();
}
```

模型调工具时,实际通过 MCP 协议发到 server,不是直接调 Java 方法。

## 3 个 Demo

### Demo 1: 任务管理

```bash
curl "http://localhost:8080/mcp/demo/ask?question=创建一个新任务,标题写周报,描述写本周 Phase 4 完成"
```

流程:
1. LLM 收到问题
2. 看到有 `createTask` 工具,准备调用
3. 通过 MCP JSON-RPC 协议发到 server
4. server 调 `productivityTools.createTask(...)`
5. 返回 ID → LLM 整理成自然语言

### Demo 2: 日历查询

```bash
curl "http://localhost:8080/mcp/demo/ask?question=今天有什么会议?"
```

`getCalendarEvents` 返回 mock 列表,LLM 整理成"今天 09:00 晨会,14:00 客户对接,16:30 代码 review"。

### Demo 3: 单位换算(纯计算)

```bash
curl "http://localhost:8080/mcp/demo/ask?question=把 100 摄氏度转换为华氏度"
```

`convertUnit(100, "celsius", "fahrenheit")` → "100.00°C = 212.00°F"。

## Streamable HTTP vs SSE

| 维度 | 旧 SSE(2024) | **Streamable HTTP (2025+)** |
|------|--------------|----------------------------|
| 端点 | `/sse` + `/messages` 二合一 | `/mcp/messages` 单一端点 |
| 传输 | Server-Sent Events 单向 | HTTP POST + 双向流 |
| 状态 | 有状态(连接保持) | **可无状态** (更易部署) |
| Spring AI | 已废弃 | **2.0 默认** |

## 踩坑(3 大常见)

### 坑 1: Server 和 Client 端口冲突

如果 server 跟 client 在同进程,server 用一个端口,client 连另一个(或 `localhost:<server-port>`)。

### 坑 2: stdio client 命令找不到

```yaml
# ❌ 路径错或命令没在 PATH
command: my-mcp-server

# ✅ 绝对路径 + 完整命令
command: /usr/local/bin/my-mcp-server
```

### 坑 3: Claude Desktop 配 MCP

`~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "spring-ai-prod": {
      "url": "http://localhost:8080/mcp/messages"
    }
  }
}
```

Streamable HTTP 用 `url`,stdio 用 `command`。

## 0 网络测试

8 tests 直接 `new ProductivityTools()`,调每个工具方法,验证返回值,跟章节 13/14 一样不依赖网络。

## 实战清单

- [x] `spring-ai-starter-mcp-server-webmvc` 暴露
- [x] `spring-ai-starter-mcp-client` 连接
- [x] `@Tool` 自动注册
- [x] Streamable HTTP transport
- [ ] **生产补 1**:MCP 鉴权(API key / OAuth)
- [ ] **生产补 2**:多 MCP server 聚合
- [ ] **生产补 3**:stdio → HTTP 转换器

## 完整代码

[03-agent/15-mcp/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/03-agent/15-mcp)

## 学完下一步

[16 Multi-Agent →](16-multi-agent.md) — Orchestrator-Workers 模式,4 个 sub-agent 协作。
