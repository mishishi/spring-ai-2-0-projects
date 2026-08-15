# 第 19 章 · Agent Production(生产化最后一公里)


## 一句话总结

**4 大生产支柱:Streaming 流式 + Observability 监控 + SafeMath 安全 + Fallback 兜底** — 把 Demo 级 Agent 变成可上线的服务。Chat Memory 多轮记忆见 ch15。

## 你将学到

- ✅ Server-Sent Events 流式响应(降低首字延迟)
- ✅ Spring Boot Actuator + Micrometer 暴露 AI 指标
- ✅ Spring AI 2.0 内置 `ai.chat.client.call` / `ai.vector.store.query` 等指标
- ✅ `SafeMathTools` 输入验证 / 白名单 / 错误处理
- ✅ `try-catch` fallback 兜底
- ✅ 限流 / 熔断 / 降级三件套(Resilience4j 简介)

## 快速开始

```bash
cd 03-agent/19-agent-production
mvn test                          # 0 网络 5 tests
mvn spring-boot:run

# 流式响应
curl "http://localhost:8080/agent/stream?conversationId=user-123&message=讲个笑话"

# 同步 + fallback
curl "http://localhost:8080/agent/ask?conversationId=user-123&message=我叫小明"

# Actuator 指标
curl "http://localhost:8080/actuator/metrics/ai.chat.client.call"
```

## 关键 API

### 1. Streaming 流式响应

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(
        @RequestParam String conversationId,
        @RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .stream()              // ← 改 .stream() 而不是 .call()
            .content();
}
```

**好处**:
- 首字延迟从 ~3s 降到 ~200ms(模型开始出 token 立刻推给前端)
- 用户体验好(打字机效果)
- WebFlux / SSE 标准协议

**完整 Streaming 实战**见 [第 6 章 · Streaming →](06-streaming.md)。

### 2. Observability 监控(Spring Boot Actuator)

ch19 的 pom 已包含 `spring-boot-starter-actuator`,启动后自动暴露指标。

**application.yml 配置**:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

启动后访问:
- `http://localhost:8080/actuator/health` — 健康检查
- `http://localhost:8080/actuator/metrics` — 所有指标
- `http://localhost:8080/actuator/metrics/ai.chat.client.call` — **Spring AI LLM 调用指标**
- `http://localhost:8080/actuator/prometheus` — Prometheus 格式(给 Grafana 抓)

**Spring AI 2.0 内置指标**(开箱即用,无需自己写):

| 指标名 | 含义 |
|---|---|
| `ai.chat.client.call` | LLM 调用次数 / 延迟 |
| `ai.vector.store.query` | 向量检索次数 / 延迟 |
| `ai.embeddings.call` | embedding 调用次数 / 延迟 |
| `ai.rag.advisor.before` | RAG advisor 触发次数 |
| `ai.rag.advisor.after` | RAG advisor 完成次数 |
| `ai.tool.call` | @Tool 调用次数(ch4/14 工具) |

**生产用法**:Prometheus 抓 `/actuator/prometheus` → Grafana Dashboard 显示:
- LLM 调用 QPS / P50 / P99 延迟
- embedding 调用成本(token 数 × 模型单价)
- RAG 命中率(retrieve 文档数 / query 数)
- 错误率(4xx / 5xx)

### 3. SafeMathTools 安全工具

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

### 4. Fallback 兜底(2 层)

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
    } catch (RateLimitExceededException e) {
        return Map.of("status", "rate_limited", "response", "当前查询繁忙,请稍后再试");
    } catch (Exception e) {
        log.error("Agent error", e);
        return Map.of("status", "fallback", "response", "抱歉,服务暂时不可用");
    }
}
```

**原则**:LLM 失败不能让用户看到 stacktrace,统一返回"安全"回复。

**2 层降级**:
- 限流降级:`RateLimitExceededException` → 友好提示
- 错误降级:`Exception` → "服务暂不可用"

## 3 个 Demo

### Demo 1: 多轮对话 + 流式

```bash
# 流式
curl -N "http://localhost:8080/agent/stream?conversationId=user-123&message=讲个长故事"
```

`-N` 关闭 curl 缓冲,看到 SSE 流(每行一个 token)。

### Demo 2: 同步 + Fallback

```bash
# 触发 fallback(临时关 OpenAI)
OPENAI_API_KEY=invalid curl "http://localhost:8080/agent/ask?message=hi"
# → {"status":"fallback","response":"抱歉,服务暂时不可用"}
```

### Demo 3: SafeMathTools 防注入

LLM 调 `safeAdd("hello", "world")`,工具返回 "错误: 输入必须是数字",不会 NPE 也不会注入。

## Resilience4j 限流 / 熔断(简介)

`ch19/pom.xml` 没默认依赖 resilience4j,生产建议加:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
```

```java
@RateLimiter(name = "agent-api", fallbackMethod = "rateLimitFallback")
public String ask(String question) {
    return chatClient.prompt().user(question).call().content();
}

private String rateLimitFallback(String question, Throwable t) {
    return "请求太频繁,请稍后再试";
}
```

```yaml
resilience4j:
  ratelimiter:
    instances:
      agent-api:
        limit-for-period: 100        # 100 QPS
        limit-refresh-period: 1s
        timeout-duration: 0          # 立即拒绝
```

**3 件套**:
- `@RateLimiter` — 限流(防刷)
- `@Retry` — 重试(临时故障)
- `@CircuitBreaker` — 熔断(下游挂时快速失败)

**完整 RAG Resilience4j 实战**见 [第 12 章 · RAG Production →](12-rag-production.md)。

## 踩坑(4 大常见)

### 坑 1: Streaming 忘了 `produces`

```java
// ❌ 默认 application/json,前端拿不到流
@GetMapping("/stream")
public Flux<String> stream(...) { ... }

// ✅ 显式 SSE
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
```

### 坑 2: 工具返回 NPE 让模型困惑

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

### 坑 3: Actuator 暴露了敏感端点

```yaml
# ❌ include: '*' — 暴露所有,security 风险
management:
  endpoints:
    web:
      exposure:
        include: '*'

# ✅ 白名单
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### 坑 4: 没监控 LLM 成本

LLM 调一次 gpt-4o 大约 $0.01,1 万次 = $100。**没监控会被刷爆**。

```java
@Component
public class CostAdvisor implements CallAdvisor {
    private final MeterRegistry registry;

    @Override
    public ChatClientResponse after(ChatClientResponse response) {
        // 估算成本(简化:按 token 数 × 模型单价)
        int tokens = response.chatResponse().getMetadata().getUsage().getTotalTokens();
        registry.counter("llm.cost.usd").increment(tokens * 0.00001);
        return response;
    }
}
```

## 0 网络测试

5 tests:
- `SafeMathTools.safeAdd` 数字验证
- `SafeMathTools.whitelistReverse` 白名单
- `SafeMathTools NPE 防护`
- `SafeMathTools 多 case 边界`
- Spring Context 加载(0 网络)

## 实战清单

- [x] Streaming 流式响应
- [x] Actuator 暴露 AI 指标
- [x] `SafeMathTools` 输入验证 + 白名单
- [x] try-catch fallback(2 层降级)
- [ ] **生产补 1**:Resilience4j 限流 / 熔断 / 重试
- [ ] **生产补 2**:Grafana Dashboard + Prometheus Alert
- [ ] **生产补 3**:LLM 成本监控 + 限额告警
- [ ] **生产补 4**:多模型 fallback(gpt-4o 挂了切 gpt-4o-mini)

## 🎉 Phase 3 (Agent 实战) 完结

**7 个 chapter**(13-19)全完成!Phase 3 串起来的能力:
- 13: Agent 基础(ChatClient + @Tool)
- 14: @Tool 5 大特性
- **15: Chat Memory 多轮对话** ← 本次新增
- 16: MCP(Anthropic 协议)
- 17: Multi-Agent 协作
- 18: Graph 状态机
- **19: 生产化(Streaming / Observability / SafeMath / Fallback)** ← 本次升级

下一站:**Phase 4 完整项目实战**(P1-P5 端到端应用)

## 完整代码

[03-agent/19-agent-production/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/03-agent/19-agent-production)

## 学完下一步

[Phase 4 项目实战 →](overviews/phase-4.md) — 5 个端到端 Spring Boot 项目,把前 3 阶段能力整合。
[真实 LLM 接入 →](guides/00-真实LLM接入.md) — 把 mock 切到 OpenAI / 通义 / DeepSeek / Ollama。
