# 第 2 章 · ChatClient API 深入

> 🎯 目标:掌握 `ChatClient` fluent API 的 4 个核心方法

## 你将学到

- ✅ `ChatClient` 完整调用链:`prompt()` / `user()` / `system()` / `options()` / `call()` / `stream()` / `content()`
- ✅ 同步调用 vs 流式调用的区别
- ✅ system message 怎么影响 LLM 行为(角色扮演)
- ✅ 4 种 chat options:`model` / `temperature` / `max-tokens` / `top-p`

## 快速开始

### 前置条件

- Java 17+
- Maven 3.8+
- `OPENAI_API_KEY`(或改 `application.yml` 用别的 LLM)

### 跑

```bash
cd 01-basics/02-chatclient-api
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

你会看到 3 个 demo 顺序跑完(每个用 `══════` 分隔):

```
══════ Demo 1: 基本 user prompt ══════
🤖 Spring AI 2.0 是 Spring 官方的 AI 框架...

══════ Demo 2: system 角色扮演 ══════
🎭 你把 @Lazy 注释当摆设,看看你的 applicationContext...

══════ Demo 3: 流式响应(streaming) ══════
🤖 streaming:
   chunk: 「Spring」
   chunk: 「 AI 」
   chunk: 「是一」
   ...
   (end of stream)
```

## 关键代码解读

`Application.java` 是核心:

```java
ChatClient client = builder.build();

// ─── Demo 1: 基本 user ──
String r1 = client.prompt()
    .user("用一句话介绍 Spring AI 2.0")
    .call()
    .content();

// ─── Demo 2: system 角色扮演 ──
String r2 = client.prompt()
    .system("你是一个毒舌但靠谱的 Java 架构师,回答简短")
    .user("Spring Boot 启动慢怎么办?")
    .call()
    .content();

// ─── Demo 3: 流式 ──
client.prompt()
    .user("用三句话介绍 Spring AI 的核心概念")
    .stream()
    .content()
    .doOnNext(chunk -> log.info("chunk: {}", chunk))
    .blockLast();
```

## ChatClient API 速查表

| 方法 | 链上位置 | 作用 |
|---|---|---|
| `ChatClient.Builder` | 起点 | Spring AI 自动注入的 bean |
| `client.prompt()` | 入口 | 构造 prompt |
| `.user(String)` | prompt | 加 user message |
| `.user(text -> text.param(...))` | prompt | 参数化 user message(后续 chapter) |
| `.system(String)` | prompt | 加 system message(影响 LLM 行为) |
| `.messages(...)` | prompt | 加完整 message 列表(多轮对话,chapter 4) |
| `.options(ChatOptions)` | prompt | 覆盖默认 chat options(per-call) |
| `.call()` | 同步入口 | 同步调用 LLM |
| `.stream()` | 流式入口 | 流式调用 LLM,返回 Flux |
| `.content()` | 终端 | 拿 `String` 结果(同步 / 流式都支持) |
| `.entity(Class)` | 终端 | 拿强类型结果(chapter 5) |
| `.chatResponse()` | 终端 | 拿完整 `ChatResponse`(包含 metadata) |

## 4 种 Chat Options

| Option | 取值 | 作用 |
|---|---|---|
| `model` | `gpt-4o-mini` / `gpt-4o` / `o1` | 选哪个 LLM |
| `temperature` | `0.0-2.0` | 创造性(0=确定性,2=发散) |
| `max-tokens` | `1-N` | 最大输出 token 数 |
| `top-p` | `0.0-1.0` | 核采样概率(0.1=只考虑前 10% 概率的 token) |

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
          max-tokens: 500
          top-p: 0.9
```

per-call 覆盖(不动全局配置):
```java
client.prompt()
    .options(ChatOptions.builder()
        .withModel("gpt-4o")
        .withTemperature(0.0)
        .build())
    .user("...")
    .call().content();
```

## 同步 vs 流式

| 维度 | `call()` 同步 | `stream()` 流式 |
|---|---|---|
| 返回类型 | `String` | `Flux<String>` |
| 何时拿结果 | 完整生成后 | 边生成边拿 |
| 适合场景 | 短回答 / 批处理 | 长回答 / 实时聊天 / Web UI 打字机效果 |
| 性能 | 简单 | 需要 WebFlux(后续 chapter 讲) |
| Token 消耗 | 一样 | 一样(只跟 LLM 行为有关) |

## 4 种 system message 模式

```java
// 1. 角色设定
.system("你是一个毒舌但靠谱的 Java 架构师")

// 2. 输出格式约束
.system("用 JSON 格式回答,key 为 name/age")

// 3. 知识边界
.system("只回答 Spring AI 相关问题,其他问题说'不在我知识范围'")

// 4. 风格控制
.system("用通俗比喻,避免术语,目标读者是非技术人员")
```

## 测试

```bash
mvn test
```

**纯本地,0 网络**(跟 chapter 1 一样的策略)。

## 目录结构

```
02-chatclient-api/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/chatclient/
    │   │   └── Application.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/cc/misshi/springai/chatclient/
            └── ApplicationTests.java
```

## 下一章预告

- [第 3 章 · Prompt 与 Advisor →](../03-prompt-advisor/README.md)
  - `PromptTemplate` 参数化 prompt
  - `Advisor` 链(类比 Spring AOP)
  - 日志 / 安全 / RAG 的 advisor 模式
