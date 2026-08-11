# 第 18 章 · Agent Production(生产化最后一公里)


## 一句话总结

**3 大生产支柱:ChatMemory 多轮记忆 + Streaming 流式 + SafeMath 工具安全** — 把 Demo 级 Agent 变成可上线的服务。

## 你将学到

- ✅ `MessageWindowChatMemory` 滑动窗口多轮对话
- ✅ `MessageChatMemoryAdvisor` 自动注入历史
- ✅ Server-Sent Events 流式响应(降低首字延迟)
- ✅ `SafeMathTools` 输入验证 / 白名单 / 错误处理
- ✅ `try-catch` fallback 兜底

## 快速开始

```bash
cd 03-agent/18-agent-production
mvn test                          # 0 网络 5 tests
mvn spring-boot:run

# 流式响应
curl "http://localhost:8080/agent/stream?conversationId=user-123&message=讲个笑话"

# 同步 + fallback
curl "http://localhost:8080/agent/ask?conversationId=user-123&message=我叫小明"
```

## 关键 API

### 1. `MessageWindowChatMemory` 多轮记忆

```java
@Configuration
public class ChatMemoryConfig {
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)   // 保留最近 10 条
                .build();
    }
}
```

**核心**:
- `ChatMemory` 是接口
- `MessageWindowChatMemory` 是 Spring AI 2.0 官方实现(替代已删除的 `InMemoryChatMemory`)
- 未来:JDBC / Redis 持久化(社区版已有)

### 2. `MessageChatMemoryAdvisor` 自动注入

```java
ChatClient chatClient = builder
        .defaultAdvisors(
            MessageChatMemoryAdvisor.builder(memory).build()
        )
        .build();

// 调用时传 conversationId,自动加载历史
chatClient.prompt()
        .user(message)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();
```

**Spring AI 内部自动**:
- 每次 LLM 调用前,从 memory 拿该 conversationId 的历史消息
- 拼到 prompt 里
- 拿到 LLM 响应后,把 user + assistant 消息存回 memory

### 3. Streaming 流式响应

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(
        @RequestParam String conversationId,
        @RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .stream()
            .content();   // 改 .stream() 而不是 .call()
}
```

**好处**:
- 首字延迟从 ~3s 降到 ~200ms(模型开始出 token 立刻推给前端)
- 用户体验好(打字机效果)
- WebFlux / SSE 标准协议

### 4. SafeMathTools 安全工具

```java
@Component
public class SafeMathTools {
    private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    @Tool(description = "安全加法:输入必须是数字")
    public String safeAdd(
            @ToolParam(description = "第一个数,必须匹配 -?\\d+(\\.\\d+)?") String a,
            @ToolParam(description = "第二个数") String b) {
        if (!NUMERIC.matcher(a).matches() || !NUMERIC.matcher(b).matches()) {
            return "错误: 输入必须是数字";
        }
        return String.valueOf(Double.parseDouble(a) + Double.parseDouble(b));
    }

    @Tool(description = "白名单字符串反转:只接受 a-zA-Z")
    public String whitelistReverse(
            @ToolParam(description = "字符串,只接受 a-zA-Z") String input) {
        if (input == null || !input.matches("[a-zA-Z]+")) {
            return "错误: 只能反转英文字符串";
        }
        return new StringBuilder(input).reverse().toString();
    }
}
```

**3 大安全考虑**:
1. **输入验证** — regex 防注入
2. **白名单** — 只允许特定输入
3. **错误处理** — 返回错误字符串,不抛异常冒泡到模型

### 5. Fallback 兜底

```java
@GetMapping("/ask")
public Map<String, Object> ask(...) {
    try {
        String response = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return Map.of("status", "ok", "response", response);
    } catch (Exception e) {
        // 不暴露内部错误
        return Map.of("status", "fallback", "response", "抱歉,服务暂时不可用");
    }
}
```

**原则**:LLM 失败不能让用户看到 stacktrace,统一返回"安全"回复。

## 3 个 Demo

### Demo 1: 多轮对话 + 记忆

```bash
# 第一轮
curl "http://localhost:8080/agent/ask?conversationId=user-123&message=我叫小明"

# 第二轮(LLM 应该记得)
curl "http://localhost:8080/agent/ask?conversationId=user-123&message=我叫什么?"
```

第二轮 LLM 看到第一轮的 user="我叫小明" + assistant 回应,自动拼到 prompt,能正确回答"你叫小明"。

### Demo 2: 流式响应

```bash
curl -N "http://localhost:8080/agent/stream?conversationId=user-123&message=讲个长故事"
```

`-N` 关闭 curl 缓冲,看到 SSE 流(每行一个 token)。

### Demo 3: SafeMathTools 防注入

LLM 调 `safeAdd("hello", "world")`,工具返回 "错误: 输入必须是数字",不会 NPE 也不会注入。

## 踩坑(3 大常见)

### 坑 1: `conversationId` 缺失

```java
// ❌ 不传 conversationId,所有用户共享一个对话
chatClient.prompt().user(message).call().content();

// ✅ 总传 conversationId
.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
```

### 坑 2: Streaming 忘了 `produces`

```java
// ❌ 默认 application/json,前端拿不到流
@GetMapping("/stream")
public Flux<String> stream(...) { ... }

// ✅ 显式 SSE
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
```

### 坑 3: 工具返回 NPE 让模型困惑

```java
// ❌ NPE 冒泡到模型,模型会卡住
@Tool
public String divide(double a, double b) {
    return String.valueOf(a / b);  // b=0 → Infinity
}

// ✅ 主动 catch + 返回错误字符串
@Tool
public String divide(double a, double b) {
    if (b == 0) return "错误: 除数不能为 0";
    return String.valueOf(a / b);
}
```

## 0 网络测试

5 tests:
- `SafeMathTools.safeAdd` 数字验证
- `SafeMathTools.whitelistReverse` 白名单
- `ChatMemory 滑动窗口`(直接 new,验证 maxMessages)
- `SafeMathTools NPE 防护`
- `SafeMathTools 多 case 边界`

## 实战清单

- [x] `MessageWindowChatMemory` 多轮记忆
- [x] `MessageChatMemoryAdvisor` 自动注入
- [x] Streaming 流式响应
- [x] `SafeMathTools` 输入验证 + 白名单
- [x] try-catch fallback
- [ ] **生产补 1**:Resilience4j 限流 + 熔断
- [ ] **生产补 2**:Memory JDBC 持久化(重启不丢)
- [ ] **生产补 3**:Actuator 指标(章节 12 已讲,接 AI metrics)

## 🎉 Phase 3 (Agent 实战) 完结

6 个 chapter 全完成!Phase 3 串起来的能力:
- 13: Agent 基础(ChatClient + @Tool)
- 14: @Tool 5 大特性
- 15: MCP(Anthropic 协议)
- 16: Multi-Agent 协作
- 17: Graph 状态机
- 18: 生产化(记忆/流式/安全)

下一站:**Phase 4 完整项目实战**(P1-P5 端到端应用)

## 完整代码

[03-agent/18-agent-production/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/03-agent/18-agent-production)

## 学完下一步

[Phase 4 项目实战 →](overviews/phase-4.md) — 5 个端到端 Spring Boot 项目,把前 3 阶段能力整合。
