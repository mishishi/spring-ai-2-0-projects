# 第 1 章 · Hello World

> Phase 1 · 基础筑基
> 🎯 跑起来,看到 Spring AI 2.0 输出第一行

## 一句话总结

Spring AI 2.0 让 Java 工程师用熟悉的 Spring 风格,5 分钟接入任何 LLM。本章跑通第一个能跑的应用。

## 读者学完能做什么

- 搭一个 Spring Boot 应用,接 LLM(OpenAI 默认)
- 用 `ChatClient` 调一次 LLM,拿字符串响应
- 换 LLM(改 pom 依赖 + application.yml)

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

### 方式 2:OpenAI 兼容(DeepSeek / 月之暗面 / Groq)

DeepSeek 是 OpenAI 兼容 API,直接复用 OpenAI starter:

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

## 测试(纯本地)

```bash
mvn test
```

`@ActiveProfiles("test")` + `@Profile("!test")` 互相屏蔽 CommandLineRunner,Spring context 启动不调 LLM,测试**0 网络**。

## 踩坑预警(本章没踩到,但相关)

| 坑 | 现象 | 解决 |
|---|---|---|
| 忘了 export `OPENAI_API_KEY` | 启动报 401 | export 或者改 `application.yml` 直接写 |
| Spring AI 1.x 代码 | `ChatClient` API 完全不同 | 确认依赖是 `spring-ai-bom:2.0.0` |
| `BaseUrl` 不识别 | 配置不生效 | Spring AI 2.0 用 `spring.ai.openai.base-url` |

## 完整代码

[01-basics/01-hello-world/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/01-hello-world)

## 下一步

- [第 2 章 · ChatClient API 深入 →](../01-basics/02-chatclient-api/README.md)
- [Phase 1 总览 →](../overviews/phase-1.md)(后续补)
