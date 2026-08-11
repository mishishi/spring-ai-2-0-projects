# 第 2 章 · ChatClient API 深入

> Phase 1 · 基础筑基
> 🎯 掌握 ChatClient fluent API 的 4 个核心方法

## 一句话总结

`ChatClient` 是 Spring AI 2.0 的核心 fluent API,4 个核心方法(`prompt()` / `user()` / `system()` / `options()`)+ 2 个调用模式(`call()` 同步 / `stream()` 流式),能搞定 80% 的 LLM 调用场景。

## 读者学完能做什么

- 用 `ChatClient` 调 LLM 拿字符串响应
- 加 `system` 改变 LLM 行为(角色 / 格式 / 风格)
- 选 LLM 调参(model / temperature / max-tokens)
- 同步 vs 流式,选对模式

## ChatClient 完整调用链

```
ChatClient.Builder(自动注入)
  ↓ .build()
ChatClient
  ↓ .prompt()
PromptSpec
  ↓ .user(text) / .system(text) / .messages(...) / .options(...)
CallResponseSpec                  StreamResponseSpec
  ↓ .call()                        ↓ .stream()
CallResponseSpec                  Flux<ChatResponse>
  ↓ .content() / .entity(T)        ↓ .content() / .chatResponse()
String / T                        Flux<String>
```

## 4 个核心 prompt 方法

### 1. `.user(String)` — 加用户消息

```java
client.prompt()
    .user("你好")
    .call().content();
```

### 2. `.system(String)` — 改变 LLM 行为

```java
client.prompt()
    .system("你是一个 Java 架构师,回答简短")
    .user("怎么排查 OOM?")
    .call().content();
```

### 3. `.messages(List<Message>)` — 多轮对话(chapter 4 深入)

```java
List<Message> history = List.of(
    new UserMessage("Java 是什么?"),
    new AssistantMessage("Java 是一门编程语言"),
    new UserMessage("它跟 Python 区别?")
);
client.prompt().messages(history).call().content();
```

### 4. `.options(ChatOptions)` — per-call 覆盖配置

```java
client.prompt()
    .options(ChatOptions.builder()
        .withModel("gpt-4o")          // 临时用 gpt-4o
        .withTemperature(0.0)         // 临时确定性输出
        .build())
    .user("...")
    .call().content();
```

## 同步 vs 流式

### 同步(`call()`)

```java
String reply = client.prompt().user("你好").call().content();
// 整段返回,适合短回答 / 批处理
```

### 流式(`stream()`)

```java
client.prompt().user("用 5 句话介绍 Spring AI").stream().content()
    .doOnNext(chunk -> System.out.print(chunk))
    .doOnComplete(() -> System.out.println("(end)"))
    .blockLast();
// 边生成边打印,适合长回答 / Web UI 打字机效果
```

## 4 种 Chat Options

| Option | 作用 | 推荐值 |
|---|---|---|
| `model` | 选 LLM | `gpt-4o-mini`(便宜) / `gpt-4o`(强) |
| `temperature` | 创造性 | 0(确定性) ~ 1(平衡) ~ 2(发散) |
| `max-tokens` | 输出上限 | 500-2000 |
| `top-p` | 核采样 | 0.9 |

## 4 种 system 模式

| 模式 | 例子 |
|---|---|
| 角色 | "你是一个 Java 架构师" |
| 输出格式 | "用 JSON 回答,key 是 name/age" |
| 知识边界 | "只回答 Spring 相关" |
| 风格 | "用通俗比喻,避免术语" |

## 完整代码

[01-basics/02-chatclient-api/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/02-chatclient-api)

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| `stream().content()` 不用 `.blockLast()` | 程序退出没看到输出 | 流式必须 blockLast 或 subscribe |
| `stream()` 直接 `.content()` 返回 Flux,不打印 | 看不出来流式效果 | 用 `doOnNext` 拦截每个 chunk |
| per-call `options` 不生效 | API 调的还是默认 | Spring AI 1.x 的 builder API 跟 2.0 不一样,确认版本 |
| `system` 写太长 | 浪费 token 没效果 | system 控制在 200 字内 |

## 下一步

- [第 3 章 · Prompt 与 Advisor →](03-prompt-advisor.md)
