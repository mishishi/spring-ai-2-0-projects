# 第 6 章 · Streaming


## 你将学到

- ✅ 为什么需要流式(同步 vs 流式 体感 5x 差)
- ✅ SSE 协议原理 + Spring WebFlux 实战
- ✅ 4 种实战场景:终端 / SSE / WebSocket / 客户端(浏览器)
- ✅ 浏览器端 `EventSource` 完整 demo
- ✅ 0 网络测试流式响应

## 一句话总结

`client.prompt().stream().content()` 返回 `Flux<String>`,用 WebFlux + `MediaType.TEXT_EVENT_STREAM_VALUE` 暴露成 SSE 端点,浏览器用 `EventSource` 实时接收,体验立即反馈。

## 读者学完能做什么

- 搭流式 Web 端点(SSE)
- 浏览器实时显示 LLM 输出
- 理解 SSE 协议格式
- 知道 WebFlux vs WebMVC 选择
- 终端流式输出 / WebSocket 双向流

## 为什么需要流式

LLM 调用的本质是 **HTTP 长连接 + 大响应**。OpenAI / 通义 / DeepSeek 都有非流式(整段返回)和流式(SSE,逐 token)两种 API。

**性能对比**(gpt-4o-mini,500 token 回答):

```
同步:    t=0 请求 ────────等待────  t=5s 整段出现
流式:    t=0 请求 t=0.5s 第一个字   t=1s 50%   t=2s 100%
```

**体感差**:

| 指标 | 同步 | 流式 |
|---|---|---|
| 看到第一个字 | 5s | 0.5s(10x 快) |
| 用户感知速度 | "卡" | "流畅" |
| 后端耗时 | 5s(一样) | 5s(一样) |
| 内存占用 | 整段常驻 | 增量释放 |

**结论**:**体感差完全来自"看不到进度"**,实际生成时间一样。流式 = 同样的时间,给用户即时反馈。

## 关键概念:3 个

### 概念 1:`Flux<String>`

Spring AI 2.0 流式响应返回 `Flux<String>`(Reactive Streams):

```java
Flux<String> stream = client.prompt().user("...").stream().content();
// 每个 onNext(String) 是一个 chunk(通常 5-20 token)
```

`Flux` 是 Reactor 的核心类型,代表"0-N 个元素的异步流"。

### 概念 2:SSE(Server-Sent Events)

浏览器原生支持的 **单向服务器推送** 协议,Content-Type 是 `text/event-stream`:

```
data: 你好

data: ,我是

data:  Spring

data: AI

data: [DONE]
```

- `data:` 前缀,每行一个事件
- 空行分隔不同事件
- `[DONE]` 是约定结束标记

### 概念 3:WebFlux vs WebMVC

| 维度 | WebMVC | WebFlux |
|---|---|---|
| 模型 | Servlet 阻塞 | Reactive 非阻塞 |
| 返回类型 | `String` / `ResponseEntity` | `Flux<T>` / `Mono<T>` |
| SSE 支持 | 麻烦(异步容器配置) | 原生(`TEXT_EVENT_STREAM_VALUE`) |
| 适用场景 | 传统 Web + 阻塞 IO | 高并发 + 流式 |

**流式必须用 WebFlux**。Spring AI 2.0 的 `Flux<String>` 跟 WebFlux 是天作之合。

## 3 个核心依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>   <!-- ← 关键 -->
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**注意**:`webflux` 跟 `web` 互斥,**别同时加**。同时加会启动失败。

## 3 行关键代码

### 后端 SSE 端点

```java
@RestController
class StreamController {
    private final ChatClient client;
    StreamController(ChatClient.Builder b) { this.client = b.build(); }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String q) {
        return client.prompt().user(q).stream().content();
    }
}
```

3 行核心:
1. `@RestController` + `@GetMapping`
2. `produces = MediaType.TEXT_EVENT_STREAM_VALUE` — Content-Type 是 SSE
3. 返回 `Flux<String>` — Spring 自动按 SSE 协议序列化

### 浏览器端 EventSource

```html
<!DOCTYPE html>
<html>
<body>
<div id="output"></div>
<script>
const es = new EventSource("/stream?q=你好");
const out = document.getElementById("output");
es.onmessage = (e) => {
    out.innerText += e.data;     // 逐 chunk 追加
};
es.onerror = () => es.close();
</script>
</body>
</html>
```

## 4 个实战场景

### 场景 1:终端 CLI 流式输出

```java
// ApplicationRunner,启动后调一次,看流式效果
@SpringBootApplication
class App implements ApplicationRunner {
    private final ChatClient client;
    App(ChatClient.Builder b) { this.client = b.build(); }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        client.prompt().user("写一首关于春天的短诗")
            .stream().content()
            .doOnNext(System.out::print)   // 边生成边打印
            .doOnComplete(() -> System.out.println("\n(end)"))
            .blockLast();
    }
}
```

运行:
```bash
mvn spring-boot:run
# 输出:
# 春风吹绿柳  ...
# (end)
```

### 场景 2:WebFlux SSE 端点(浏览器)

见上文 "3 行关键代码"。

**curl 测试 SSE**:

```bash
curl -N "http://localhost:8080/stream?q=你好"
# data: 你好

# data: ,我是

# data:  Spring

# data: AI
```

`-N` 关闭 curl 缓冲,实时显示。

### 场景 3:WebSocket 双向流(高级)

适合 IDE 助手 / 多人协作场景:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

```java
@Component
class LlmWebSocketHandler extends TextWebSocketHandler {
    private final ChatClient client;
    LlmWebSocketHandler(ChatClient.Builder b) { this.client = b.build(); }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage msg) {
        client.prompt().user(msg.getPayload())
            .stream().content()
            .subscribe(
                chunk -> sendChunk(session, chunk),
                err -> sendError(session, err),
                () -> sendDone(session)
            );
    }

    private void sendChunk(WebSocketSession s, String c) {
        try { s.sendMessage(new TextMessage(c)); } catch (Exception e) {}
    }
    // ... 同理 sendError / sendDone
}
```

### 场景 4:Java 客户端消费 Flux

```java
@Service
class StreamConsumer {
    private final ChatClient client;
    StreamConsumer(ChatClient.Builder b) { this.client = b.build(); }

    void processStreaming(String prompt, Consumer<String> onChunk) {
        client.prompt().user(prompt)
            .stream().content()
            .doOnNext(onChunk)
            .doOnError(e -> log.error("stream error", e))
            .doOnComplete(() -> log.info("stream done"))
            .subscribe();
    }
}

// 用法
streamConsumer.processStreaming("写一首诗", System.out::print);
```

**注意**:`subscribe()` 启动流式订阅;没 `subscribe()` 的话 Flux 不会跑(惰性求值)。

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class StreamingTest {
    @Autowired ChatClient client;

    @Test
    void testStreamChunks() {
        StepVerifier.create(
                client.prompt().user("hi").stream().content()
            )
            .expectNextMatches(s -> s != null)   // 至少一个 chunk
            .thenConsumeWhile(s -> true)         // 接受后续 chunk
            .verifyComplete();
    }
}
```

`StepVerifier` 是 Reactor 测试工具,验证 Flux 的每一步。

## 关键调优:Backpressure

Reactive 流有个问题:**生产者快,消费者慢**。LLM 生成是网络 IO,可能 chunk 来得很快。

WebFlux 默认会 **背压**(backpressure)— 告诉上游"我处理慢,慢点发"。

**调优选项**:

```java
Flux<String> stream = client.prompt().user(q)
    .stream().content()
    .limitRate(10)             // 每次最多 10 个 chunk
    .onBackpressureBuffer(100);  // 缓冲 100 个

return stream;
```

`limitRate` 跟 `onBackpressureBuffer` 解决"流得太快"问题。

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| 用 `web` 不用 `webflux` | Flux 不能直接序列化 | 加 webflux,删 web |
| 前端用 fetch + read() | SSE 接不到 | 用 `EventSource` |
| 加 CORS | 浏览器跨域 | `@CrossOrigin` |
| 流到一半断 | 浏览器没收到 `[DONE]` | 后端 catch + complete |
| 没 `subscribe()` | Flux 不跑 | 流式必须 subscribe 或 blockLast |
| `blockLast()` 在 WebFlux 控制器里用 | 阻塞 event loop | 控制器直接 return Flux,不要 block |
| chunk 太大撑爆浏览器 | 内存涨 | 后端 `limitRate(10)` + 前端 throttle |

## 实战部署清单

- [ ] 加 `webflux` 依赖,**删掉** `web`
- [ ] 跑通 SSE 端点
- [ ] `curl -N` 验证流式输出
- [ ] 写一个 HTML + `EventSource` 前端,实时显示
- [ ] 测试:断网时看后端是 fail 还是 graceful
- [ ] `mvn test` 用 `StepVerifier` 验证 Flux
- [ ] 监控:`/actuator/metrics/ai.chat.client.call` 看 LLM 调用计数

## 实战模式速查

| 场景 | 端点类型 |
|---|---|
| Web 聊天 | WebFlux + SSE |
| IDE 助手 | WebFlux + WebSocket |
| 终端 CLI | `doOnNext(System.out::print)` |
| 批处理 | 同步 `.call().content()` |
| Java 客户端 | `.subscribe(onChunk)` |

## 完整代码

[01-basics/06-streaming/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/06-streaming)

## 🎉 Phase 1 完结

第 6 章是 Phase 1(基础筑基)最后一章。

**Phase 1 完成的 6 个 chapter**:
1. Hello World
2. ChatClient API
3. Prompt + Advisor
4. Function Calling
5. Structured Output
6. Streaming ← 现在

**Phase 2 开始 RAG 实战**(embedding / vector store / 文档加载 / 高级检索 / rerank / 评估)。

## 下一步

- [第 7 章 · RAG Overview →](07-rag-overview.md)— 进入 RAG 实战
- [Phase 1 总览 →](overviews/phase-1.md)— 回顾
- 想直接看生产部署?[第 18 章 · Agent Production →](19-agent-production.md)
