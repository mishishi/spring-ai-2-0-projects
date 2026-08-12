# 第 1 章 · Hello World


## 你将学到

- ✅ 搭一个 Spring Boot 4.0 + Spring AI 2.0 应用,5 分钟跑通
- ✅ `ChatClient` 同步调一次 LLM,拿字符串响应
- ✅ 换 LLM provider(OpenAI / 通义 / DeepSeek / Ollama)
- ✅ 多轮对话 + system prompt + 参数调优
- ✅ 0 网络测试模式(mock fallback)

## 一句话总结

Spring AI 2.0 让 Java 工程师用熟悉的 Spring 风格,5 分钟接入任何 LLM。本章跑通第一个能跑的应用。

## 读者学完能做什么

- 搭一个 Spring Boot 应用,接 LLM(OpenAI 默认)
- 用 `ChatClient` 调一次 LLM,拿字符串响应
- 多轮对话(保留上下文)
- 用 system prompt 控制 LLM 行为
- 改 temperature / model / max-tokens 调优
- 换 LLM(改 env vars,不改代码)

## 5 分钟上手

### 1. 配 API key

```bash
export OPENAI_API_KEY=sk-xxxxxx
```

### 2. 跑

```bash
cd 01-basics/01-hello-world
mvn spring-boot:run
```

### 3. 看输出

```
🤖 Spring AI says: Spring AI 2.0 是 Spring 官方的 AI 框架...
```

完事。这就是 Hello World。

## 为什么需要 Spring AI

在 Spring AI 出现之前,Java 工程师接 LLM 通常是:

1. **手撕 HTTP**:用 `RestTemplate` 拼各家 API(OpenAI / Claude / 通义),文档不一致
2. **接 Python 微服务**:用 FastAPI 包一层,Java 这边只调 HTTP — 部署链 +1
3. **用社区库**:`langchain4j` 跟 Spring 生态不够"原生",有些 API 跟 Spring 风格割裂

**Spring AI 2.0 的定位**:Spring 官方的 AI 抽象层,**跟 `JdbcTemplate` / `RestTemplate` 一样** —

| Spring 抽象 | 类比 | Spring AI 抽象 |
|---|---|---|
| `JdbcTemplate` | 屏蔽 JDBC driver 差异 | `ChatClient` 屏蔽 LLM provider 差异 |
| `RestTemplate` | 统一 HTTP 调用 | `EmbeddingClient` 统一 embedding 调用 |
| `DataSource` | 配置驱动切换 | `spring.ai.openai.*` 配置驱动切换 provider |

**类比 `JdbcTemplate`**:

```java
// 写 JdbcTemplate 时,你不关心底层是 MySQL 还是 PostgreSQL
jdbcTemplate.query("SELECT * FROM user WHERE id = ?", id);

// 写 ChatClient 时,你不关心底层是 GPT-4 还是 Qwen
chatClient.prompt().user("你好").call().content();
```

`pom.xml` 换依赖,`application.yml` 改配置,**业务代码完全不动**。

## 关键概念(3 个)

### 概念 1:`ChatClient`

Spring AI 2.0 核心抽象,统一所有 LLM 的调用接口。

```java
ChatClient client = chatClientBuilder.build();
String response = client.prompt()
    .user("你好")
    .call()
    .content();
```

`ChatClient.Builder` 是 Spring AI 自动注入的 bean,不用自己 new。

### 概念 2:`prompt()`

`prompt()` 返回一个 `PromptSpec`,链式 API 构造 prompt:

| 方法 | 作用 |
|---|---|
| `.user(text)` | 加 user message |
| `.system(text)` | 加 system message(后面 chapter 讲) |
| `.messages(...)` | 加完整 message 列表 |
| `.options(...)` | 设 LLM 参数(temperature / model) |

### 概念 3:`call()` vs `stream()`

| 方法 | 行为 | 何时用 |
|---|---|---|
| `.call().content()` | 同步等结果 | 简单调用 |
| `.stream().content()` | 流式输出(打字机效果) | 第 6 章讲 |

## 关键代码(完整版)

### 1. 单轮对话

```java
@Service
class HelloService {
    private final ChatClient chatClient;

    HelloService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    String greet(String name) {
        return chatClient.prompt()
            .user("用一句话问候 %s".formatted(name))
            .call()
            .content();
    }
}
```

### 2. 多轮对话(保留上下文)

```java
@Service
class ChatService {
    private final ChatClient chatClient;

    String multiTurn(List<Message> history, String newMessage) {
        return chatClient.prompt()
            .messages(history)              // 旧消息 + 新消息
            .call()
            .content();
    }
}
```

`Message` 是 Spring AI 的统一消息类型,3 个子类:

- `UserMessage` — 用户消息
- `SystemMessage` — 系统消息(给 LLM 立规矩)
- `AssistantMessage` — LLM 历史回复(用于多轮)

### 3. System Prompt(立人设)

```java
String reply = chatClient.prompt()
    .system("""
        你是一个 Java 高级工程师,回答要专业、简洁,带代码示例。
        如果问题不清楚,反问而不是瞎猜。
        """)
    .user("什么是 Spring AI 的 ChatClient?")
    .call()
    .content();
```

**System prompt 写法技巧**:
- 用 """ 三引号写多行,清晰
- 明确"做什么"和"不做什么"
- 给反问/拒答的具体规则,避免幻觉

### 4. 改参数(温度 / 模型 / token 上限)

```java
String reply = chatClient.prompt()
    .user("写一首关于春天的诗")
    .options(ChatOptions.builder()
        .model("gpt-4o")
        .temperature(0.8)       // 0=确定性,1=随机(默认),>1 乱说
        .maxTokens(200)          // 输出上限
        .build())
    .call()
    .content();
```

或者 application.yml 配:

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.0
          max-tokens: 500
```

**参数选型建议**:

| 场景 | temperature | 说明 |
|---|---|---|
| 代码生成 | 0.0 | 要确定性输出 |
| 翻译 | 0.0 - 0.3 | 准确 |
| 摘要 | 0.3 - 0.5 | 平衡 |
| 创意写作 | 0.7 - 1.0 | 多样性 |
| 头脑风暴 | 1.0+ | 跳跃 |

## 换 LLM(2 种方式)

### 方式 1:换 provider

改 pom 依赖:

```xml
<!-- Ollama(本地) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3
```

### 方式 2:OpenAI 兼容(DeepSeek / 通义千问 / Groq)

这三个都提供 **OpenAI 兼容 API**,直接复用 OpenAI starter,只改 `base-url` 和 `model`:

```yaml
# DeepSeek
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat

# 通义千问
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen-plus
```

**完整 4 种 provider 切换** 详见 [实践指南 · 真实 LLM 接入](guides/00-真实LLM接入.md)。

## 测试(纯本地)

```bash
mvn test
```

`@ActiveProfiles("test")` + `@Profile("!test")` 互相屏蔽 CommandLineRunner,Spring context 启动不调 LLM,测试**0 网络**。

### 验证 mock fallback

```java
@SpringBootTest
@ActiveProfiles("test")
class HelloServiceTest {
    @Autowired HelloService service;

    @Test
    void testGreet() {
        // 没设 OPENAI_API_KEY → mock fallback → 默认回复
        String reply = service.greet("World");
        assertThat(reply).isNotBlank();
    }
}
```

**0 网络的原理**:

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:fake-key-for-tests}   # 默认值是假的
```

测试时没设环境变量 → `api-key=fake-key-for-tests` → Spring context 启动时 ChatClient **不初始化真 LLM 客户端**,service 内部 null-check 走 mock 分支。

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| 忘了 export `OPENAI_API_KEY` | 启动报 401 | export 或者改 `application.yml` 直接写 |
| Spring AI 1.x 代码 | `ChatClient` API 完全不同 | 确认依赖是 `spring-ai-bom:2.0.0` |
| `BaseUrl` 不识别 | 配置不生效 | Spring AI 2.0 用 `spring.ai.openai.base-url` |
| `mvn test` 调了真 LLM | 测试超时 + 烧钱 | 检查 `@ActiveProfiles("test")` 写没写 |
| system prompt 冲突 | 同一项目多个 system 风格混乱 | 抽到 `application.yml` 统一管理,见 ch3 |

## 实战部署清单

- [ ] export `OPENAI_API_KEY`(或换 provider 走通义 / DeepSeek)
- [ ] 跑通 `mvn spring-boot:run`
- [ ] 调一次 API 看真 LLM 返回(不是 mock fallback)
- [ ] 故意改错 API key,看 401 错误(确认链路真通)
- [ ] `mvn test` 仍然 0 网络 PASS
- [ ] 切 system prompt 看 LLM 风格变化
- [ ] 改 temperature 0.0 → 1.0 看输出多样性变化

## 完整代码

[01-basics/01-hello-world/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/01-hello-world)

## 下一步

- [第 2 章 · ChatClient API 深入 →](02-chatclient-api.md)
- [Phase 1 总览 →](overviews/phase-1.md)
- 想直接切真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
