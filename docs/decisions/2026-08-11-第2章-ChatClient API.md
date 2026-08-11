# 2026-08-11 Chapter 2 - ChatClient API

## 背景

Phase 1 第 2 章,深入 `ChatClient` fluent API。

## 决策

### D-10:Chapter 2 不加 web starter

**为什么**:
- Chapter 2 重点是 `ChatClient` 4 个核心方法(`prompt` / `user` / `system` / `options`)
- 加 web 启动 Tomcat,影响测试 context 启动速度(从 ~3s 增到 ~6s)
- 流式 API 暂不接 WebFlux 端点(推到 chapter 6,跟 SSE + REST 一起讲)

**结果**:
- 3 个 demo 全在 `CommandLineRunner` 里
- 跑 `mvn spring-boot:run` 看日志
- 测试保持 `WebEnvironment.NONE`(跟 chapter 1 一致)

### D-11:流式用 `.doOnNext().blockLast()` 而不是 `.subscribe()`

**为什么**:
- `.subscribe()` 是异步的,主线程立即返回,Flux 在后台跑,可能程序退出时还没跑完
- `.blockLast()` 同步阻塞,等流式结束再继续,CommandLineRunner 不会提前退出
- Spring Boot `CommandLineRunner` 跑完就退,需要同步等待

**Pattern 复用**:
```java
client.prompt().user("...").stream().content()
    .doOnNext(chunk -> log.info("chunk: {}", chunk))
    .doOnComplete(() -> log.info("(end)"))
    .blockLast();  // 关键:CommandLineRunner 必须同步等
```

## 实际产出

### 文件清单(7 个新增 + 1 个修改)

- [x] `01-basics/02-chatclient-api/pom.xml` (1632B)
- [x] `01-basics/02-chatclient-api/src/main/java/cc/misshi/springai/chatclient/Application.java` (2513B)
- [x] `01-basics/02-chatclient-api/src/main/resources/application.yml` (303B)
- [x] `01-basics/02-chatclient-api/src/test/java/cc/misshi/springai/chatclient/ApplicationTests.java` (792B)
- [x] `01-basics/02-chatclient-api/README.md` (3750B)
- [x] `docs/02-chatclient-api.md` (2938B)
- [x] `pom.xml` (加 module,1 行变更)
- [x] `01-basics/02-chatclient-api/.gitkeep` 删除

### 验证结果

| 步骤 | 耗时 | 结果 |
|---|---|---|
| `mvn test` | 9.86s | ✅ Tests run 1, Failures 0, Errors 0 |

## Git 状态

- **Commit**:`ea2a8c4 feat: Phase 1 第 2 章 - ChatClient API 深入`
- **Push**:`6ffbc26..ea2a8c4 main -> main` ✅
- **Bundle**:`/Users/zhurenbao/Documents/spring-ai-2-0-projects-20260811-1747-chapter-2.bundle` 28.4KB

## 仓库 commit 历史(4 个)

```
ea2a8c4 feat: Phase 1 第 2 章 - ChatClient API 深入
6ffbc26 docs: 记录 W0 收尾 + Chapter 1 决策
002004f feat: W0 收尾 + Phase 1 第 1 章 Hello World
644a8de chore: 初始化仓库
```

## 下一步

Phase 1 第 3 章:`03-prompt-advisor` - PromptTemplate 参数化 + Advisor 链(类比 Spring AOP 的拦截器)
