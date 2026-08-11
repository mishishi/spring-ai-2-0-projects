# 第 6 章 · Streaming

> 🎯 目标:用 WebFlux + SSE 把 LLM 输出**实时推给浏览器**,实现打字机效果

## 你将学到

- ✅ Spring WebFlux + Spring AI 流式 API 组合
- ✅ SSE(Server-Sent Events)协议
- ✅ 浏览器端 `EventSource` 接收流
- ✅ 同步 vs 流式性能对比

## 快速开始

```bash
cd 01-basics/06-streaming
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

打开浏览器:<http://localhost:8080/>

你会看到一个简单页面:
- 输入框:问题
- 2 个按钮:**同步** / **流式**
- 输出框:LLM 的回答

**流式模式** 下,LLM 一边生成一边显示,真正的打字机效果。

## 端点

| 端点 | 方法 | 类型 | 用途 |
|---|---|---|---|
| `/` | GET | HTML | 演示页面 |
| `/api/chat/sync?q=...` | GET | String | 同步(等完整生成) |
| `/api/chat/stream?q=...` | GET | `text/event-stream` | 流式 SSE |

## 关键代码

### Controller 端点对比

```java
// 同步:等 LLM 完整生成,一次性返回 String
@GetMapping("/sync")
public String sync(@RequestParam String q) {
    return client.prompt()
        .user(q)
        .call()
        .content();   // 同步 String
}

// 流式:返回 Flux<String>,SSE 协议
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String q) {
    return client.prompt()
        .user(q)
        .stream()      // ← 流式
        .content();    // Flux<String>
}
```

### 浏览器端(vanilla JS)

```javascript
// 同步
const r = await fetch('/api/chat/sync?q=...');
const text = await r.text();

// 流式(EventSource 自动处理 SSE)
const es = new EventSource('/api/chat/stream?q=...');
es.onmessage = e => {
    output.textContent += e.data;  // 边收边显示
};
es.onerror = () => es.close();
```

## SSE 协议格式

```
data: 第一段内容\n
data: 第二段内容\n
data: 第三段内容\n
\n
```

每条消息以 `data: ` 开头,以 `\n\n` 分隔。`EventSource` 自动解析。

## WebFlux vs WebMVC

| 维度 | WebFlux | WebMVC |
|---|---|---|
| 模型 | 响应式(Reactor) | 线程池 |
| 适合 | 流式 / 长连接 / 高并发 | 短请求 / 传统 REST |
| 内存 | 少(异步) | 多(每请求一线程) |
| 启动器 | `webflux` | `web` |
| Mono/Flux | ✅ | ❌ |

**Spring AI 1.x 流式必须用 WebFlux** — 因为返回 `Flux<T>`,需要响应式支持。
**Spring AI 2.0 流式也支持 Servlet**(MVC 也能跑流式,但 WebFlux 更优)。

## 性能对比(实际数据)

| 模式 | 100 字回答 | 体感 |
|---|---|---|
| 同步 | 用户等到 3-5s,然后整段出现 | 慢 |
| 流式 | 1s 后开始显示,逐字出,2-3s 完成 | 快 |

**流式体感快 50%**,虽然总时间一样,但用户感觉"立即有反馈"。

## 实战模式

| 场景 | 实现 |
|---|---|
| Web 聊天 UI | WebFlux + SSE + EventSource |
| 实时 IDE 助手 | WebFlux + WebSocket(双向) |
| 命令行工具 | `client.stream().content().doOnNext(System.out::print)` |
| 批处理 | 同步 `.call().content()` |

## 测试

```bash
mvn test
```

`@SpringBootTest`(默认 MOCK web env)+ `@ActiveProfiles("test")` 屏蔽实际 LLM 调用,验证 context 起来。

## 目录结构

```
06-streaming/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/streaming/
    │   │   ├── Application.java
    │   │   ├── ChatController.java
    │   │   └── PageController.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/cc/misshi/springai/streaming/
            └── ApplicationTests.java
```

## 🎉 Phase 1 完结

第 6 章是 Phase 1(基础筑基)最后一章。Phase 2 开始 RAG 实战。
