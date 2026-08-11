# 第 6 章 · Streaming

> Phase 1 · 基础筑基
> 🎯 WebFlux + SSE 实现"打字机效果"

## 一句话总结

`client.prompt().stream().content()` 返回 `Flux<String>`,用 WebFlux + `MediaType.TEXT_EVENT_STREAM_VALUE` 暴露成 SSE 端点,浏览器用 `EventSource` 实时接收,体验立即反馈。

## 读者学完能做什么

- 搭流式 Web 端点
- 浏览器实时显示 LLM 输出
- 理解 SSE 协议格式
- 知道 WebFlux vs WebMVC 选择

## 同步 vs 流式性能

```
同步:    t=0 请求 ────────等待────  t=5s 整段出现
流式:    t=0 请求 t=1s 出现  t=2s 出现  ...  t=3s 结束
```

**体感**:流式 5x 快(用户视角)

## 3 个核心依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
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
```

## 3 行关键代码

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String q) {
    return client.prompt().user(q).stream().content();
}
```

## 完整代码

[01-basics/06-streaming/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/06-streaming)

## 实战模式

| 场景 | 端点类型 |
|---|---|
| Web 聊天 | WebFlux + SSE |
| IDE 助手 | WebFlux + WebSocket |
| 终端 CLI | `doOnNext(System.out::print)` |
| 批处理 | 同步 `.call().content()` |

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| 用 `web` 不用 `webflux` | Flux 不能直接序列化 | 加 webflux,删 web |
| 前端用 fetch + read() | SSE 接不到 | 用 `EventSource` |
| 加 CORS | 浏览器跨域 | `@CrossOrigin` |
| 流到一半断 | 浏览器没收到 `[DONE]` | 后端 catch + complete |

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
