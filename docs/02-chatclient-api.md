# 第 2 章 · ChatClient API 深入


## 你将学到

- ✅ `ChatClient` 完整调用链(`Builder → prompt → call/stream → content/entity`)
- ✅ 4 个核心 prompt 方法:`user()` / `system()` / `messages()` / `options()`
- ✅ 2 种调用模式:`call()` 同步 / `stream()` 流式
- ✅ 4 种 Chat Options 调参 + 4 种 system 模式
- ✅ `entity(T)` 直接拿结构化对象(下一章深入)

## 一句话总结

`ChatClient` 是 Spring AI 2.0 的核心 fluent API,4 个核心方法(`prompt()` / `user()` / `system()` / `options()`)+ 2 个调用模式(`call()` 同步 / `stream()` 流式),能搞定 80% 的 LLM 调用场景。

## 读者学完能做什么

- 用 `ChatClient` 调 LLM 拿字符串响应
- 加 `system` 改变 LLM 行为(角色 / 格式 / 风格)
- 选 LLM 调参(model / temperature / max-tokens)
- 同步 vs 流式,选对模式
- 直接拿结构化对象(`.entity(Person.class)`)

## 背景:Spring AI 1.x → 2.0 API 改了什么

如果用过 Spring AI 1.x,你可能记得这个老 API:

```java
// Spring AI 1.x(已废弃)
chatModel.call(new Prompt("你好"));
ChatResponse response = ...;
String content = response.getResult().getOutput().getContent();
```

**2.0 的简化**:

```java
// Spring AI 2.0
chatClient.prompt().user("你好").call().content();
```

对比:

| 维度 | 1.x | 2.0 |
|---|---|---|
| 入口 | `ChatModel` | `ChatClient` |
| Prompt 构造 | 手动 `new Prompt(messages)` | 链式 `.user().system()` |
| 取响应 | `response.getResult().getOutput().getContent()` | `.content()` |
| 结构化 | 自己反序列化 | `.entity(Person.class)` 一行 |
| 流式 | `Flux<ChatResponse>` 自己解析 | `.stream().content()` 直接 `Flux<String>` |

**本质**:2.0 把 Spring 的 fluent API 风格贯彻到 AI 调用,**链式 + 端点方法**(`content()` / `entity()` / `chatResponse()`)让代码读起来像自然语言。

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

**调参速记**:
- **代码生成**:`temperature=0`(要确定性,同一输入同一输出)
- **摘要**:`temperature=0.3-0.5`(平衡准确和流畅)
- **翻译**:`temperature=0`(要准确)
- **创意写作**:`temperature=0.7-1.0`(要多样性)
- **成本敏感**:`max-tokens=500` + `gpt-4o-mini`

## 4 种 system 模式

| 模式 | 例子 |
|---|---|
| 角色 | "你是一个 Java 架构师" |
| 输出格式 | "用 JSON 回答,key 是 name/age" |
| 知识边界 | "只回答 Spring 相关,不懂就说不懂" |
| 风格 | "用通俗比喻,避免术语,3 段以内" |

**system 写法的 3 个要点**:

1. **正面描述**(不要"不要做 X",改"请做 Y") — LLM 对负面指令不敏感
2. **具体可执行**(避免"回答要好",改"用 Markdown bullet 列出 3-5 点")
3. **给反问/拒答规则**("如果问题不清楚,反问而不是瞎猜")

## 关键代码:4 个实战场景

### 场景 1:单轮问答(最常用)

```java
@Service
class QaService {
    private final ChatClient client;
    QaService(ChatClient.Builder b) { this.client = b.build(); }

    String answer(String question) {
        return client.prompt().user(question).call().content();
    }
}
```

### 场景 2:多轮对话(保留上下文)

```java
@Service
class MultiTurnService {
    private final ChatClient client;
    private final List<Message> history = new ArrayList<>();

    MultiTurnService(ChatClient.Builder b) { this.client = b.build(); }

    String chat(String userMessage) {
        history.add(new UserMessage(userMessage));
        String reply = client.prompt().messages(history).call().content();
        history.add(new AssistantMessage(reply));
        return reply;
    }

    void reset() { history.clear(); }
}
```

**注意**:多轮对话**历史会越来越长**,token 成本会增加。生产环境通常用 `MessageWindowMemory`(Phase 3 ch14 讲)。

### 场景 3:结构化输出(`.entity(T)`)

```java
record Person(String name, int age) {}

@Service
class StructuredService {
    private final ChatClient client;
    StructuredService(ChatClient.Builder b) { this.client = b.build(); }

    Person extract(String text) {
        return client.prompt()
            .user("从下面文本提取人名和年龄: " + text)
            .call()
            .entity(Person.class);    // ← 关键
    }
}

// 用法
Person p = service.extract("张三今年 30 岁");
// → Person[name=张三, age=30]
```

底层 Spring AI 用 **JSON mode** 强制 LLM 输出 JSON,然后 Jackson 反序列化。详见 ch5。

### 场景 4:Per-call 覆盖参数(临时换模型)

```java
@Service
class DynamicService {
    private final ChatClient client;
    DynamicService(ChatClient.Builder b) { this.client = b.build(); }

    String answer(String question, boolean needHighQuality) {
        var options = ChatOptions.builder()
            .withModel(needHighQuality ? "gpt-4o" : "gpt-4o-mini")
            .withTemperature(0.0)
            .build();
        return client.prompt()
            .options(options)
            .user(question)
            .call()
            .content();
    }
}
```

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class ChatClientServiceTest {
    @Autowired ChatClient chatClient;

    @Test
    void testBasic() {
        // 没设 OPENAI_API_KEY → 走 mock fallback
        String reply = chatClient.prompt().user("hi").call().content();
        assertThat(reply).isNotBlank();
    }

    @Test
    void testWithSystem() {
        String reply = chatClient.prompt()
            .system("用一句话回答")
            .user("Java 是什么?")
            .call()
            .content();
        assertThat(reply).isNotBlank();
    }
}
```

`@ActiveProfiles("test")` 让 application-test.yml 生效,所有 env vars 走 mock 默认值。

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| `stream().content()` 不用 `.blockLast()` | 程序退出没看到输出 | 流式必须 blockLast 或 subscribe |
| `stream()` 直接 `.content()` 返回 Flux,不打印 | 看不出来流式效果 | 用 `doOnNext` 拦截每个 chunk |
| per-call `options` 不生效 | API 调的还是默认 | Spring AI 1.x 的 builder API 跟 2.0 不一样,确认版本 |
| `system` 写太长 | 浪费 token 没效果 | system 控制在 200 字内 |
| `.entity(T)` LLM 偶尔不返回 JSON | 解析报错 | 跟 ch5 一样,加 `@JsonProperty` 兜底 |
| 多轮对话 history 无限增长 | token 爆 | 用 MessageWindowMemory 限到最近 10-20 轮 |

## 实战部署清单

- [ ] 跑通单轮 + 多轮 + system 三个场景
- [ ] 试 `.entity(Person.class)` 拿结构化对象
- [ ] `temperature` 0.0 vs 1.0 对比输出差异
- [ ] 临时切 `gpt-4o` 跑一个 per-call options 例子
- [ ] 流式输出用 `doOnNext` 看 chunk
- [ ] `mvn test` 0 网络 PASS

## 完整代码

[01-basics/02-chatclient-api/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/02-chatclient-api)

## 下一步

- [第 3 章 · Prompt 与 Advisor →](03-prompt-advisor.md)— system 模板化 + Advisor 拦截
- [第 5 章 · Structured Output →](05-structured-output.md)— `.entity()` 深入
- [第 6 章 · Streaming →](06-streaming.md)— 流式完整实战
