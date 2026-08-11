# 第 1 章 · Hello World

> 🎯 目标:跑起来,看到 Spring AI 2.0 输出第一行

## 你将学到

- ✅ Spring AI 2.0 第一个能跑的 app
- ✅ `ChatClient` 的 fluent API(`builder()` / `prompt()` / `call()` / `content()`)
- ✅ 怎么配置 OpenAI / 怎么换 LLM

## 快速开始

### 前置条件

- Java 17+
- Maven 3.8+
- OpenAI API key(或别的 LLM 提供方)

### 配置 API key

```bash
export OPENAI_API_KEY=sk-xxxxxx
```

### 跑起来

```bash
cd 01-basics/01-hello-world
mvn spring-boot:run
```

你会看到类似输出:

```
🤖 Spring AI says: Spring AI 2.0 是 Spring 官方的 AI 框架,统一 LLM 集成,让 Java 工程师 5 分钟接入 AI。
```

## 怎么换 LLM?

`spring-ai-starter-model-openai` 是 OpenAI 专用。要换模型,改 pom 依赖 + application.yml:

| 模型 | 依赖 | 配置 |
|---|---|---|
| OpenAI | `spring-ai-starter-model-openai` | `spring.ai.openai.api-key` |
| Ollama(本地) | `spring-ai-starter-model-ollama` | `spring.ai.ollama.base-url` |
| DeepSeek(OpenAI 兼容) | `spring-ai-starter-model-openai` + base-url | 见下 |
| 通义千问 | `spring-ai-starter-model-qianfan` 或自定义 | `spring.ai.qianfan.api-key` |

**DeepSeek 示例**(OpenAI 兼容 API):

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
```

更多参考 [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)。

## 测试

```bash
mvn test
```

**纯本地,0 网络**:

- ✅ `@ActiveProfiles("test")` 屏蔽 `CommandLineRunner`
- ✅ `application.yml` 默认 fake key 不会真连 LLM
- ✅ `ChatClient.Builder` 不会在测试里被调

## 关键代码解读

`Application.java` 是核心:

```java
@Bean
@Profile("!test")
CommandLineRunner helloWorld(ChatClient.Builder builder) {
    return args -> {
        ChatClient client = builder.build();
        String response = client.prompt()
                .user("用一句话介绍 Spring AI 2.0,不超过 30 字")
                .call()
                .content();
        log.info("🤖 Spring AI says: {}", response);
    };
}
```

`ChatClient` 是 Spring AI 2.0 的核心 fluent API:

| 调用 | 作用 |
|---|---|
| `ChatClient.Builder` | 自动注入(Spring AI starter 配好的) |
| `.build()` | 创建 ChatClient 实例 |
| `.prompt()` | 开始构造 prompt |
| `.user("...")` | 加 user message |
| `.call()` | 同步调用 LLM |
| `.content()` | 拿字符串响应 |

> 后面 chapter 会深入:`system()` / `messages()` / `options()` / `advisors()` 等。

## 目录结构

```
01-hello-world/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/helloworld/
    │   │   └── Application.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/cc/misshi/springai/helloworld/
            └── ApplicationTests.java
```

## 下一步

[第 2 章 · ChatClient API 深入 →](../02-chatclient-api/README.md)
