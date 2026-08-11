# 2026-08-11 Chapter 3 - Prompt + Advisor

## 背景

Phase 1 第 3 章,PromptTemplate 参数化 + Advisor 拦截器模式。

## 决策

### D-12:Chapter 3 用内置 `SimpleLoggerAdvisor`,不做自定义

**为什么**:
- 自定义 advisor 需要 `ChatClientRequest` / `ChatClientResponse` 内部 API,1.1.3 还在演进
- SimpleLoggerAdvisor 是稳定内置的,足以演示 advisor 概念
- 自定义 advisor 推到 chapter 4+(Function Calling)或 chapter 5+(RAG),有具体场景再讲

**结果**:
- chapter 3 跟 chapter 1/2 节奏一致(简单 / 0 网络)
- README 给出"自定义 advisor 模板代码",让用户知道怎么扩展

### D-13:PromptTemplate 用 `new PromptTemplate(String) + .render(Map)` 模式

**为什么**:
- Spring AI 1.1.3 跟 2.0 的 PromptTemplate API 略有不同
- 最稳的写法:1.1.3 跟 2.0 都支持
- 后续 chapter 需要模板时,可平滑升级

**Pattern**:
```java
PromptTemplate pt = new PromptTemplate(templateString);
String userInput = pt.render(Map.of("k", "v"));
client.prompt().user(userInput).call().content();
```

## 实际产出

### 文件清单(7 个新增 + 1 个修改)

- [x] `01-basics/03-prompt-advisor/pom.xml` (1523B)
- [x] `01-basics/03-prompt-advisor/src/main/java/.../Application.java` (2862B)
- [x] `01-basics/03-prompt-advisor/src/main/resources/application.yml` (232B)
- [x] `01-basics/03-prompt-advisor/src/test/java/.../ApplicationTests.java` (599B)
- [x] `01-basics/03-prompt-advisor/README.md` (3684B)
- [x] `docs/03-prompt-advisor.md` (3173B)
- [x] `pom.xml` (加 module,1 行变更)
- [x] `01-basics/03-prompt-advisor/.gitkeep` 删除

### 验证结果

| 步骤 | 耗时 | 结果 |
|---|---|---|
| `mvn test` | 4.71s | ✅ Tests run 1, Failures 0, Errors 0 |

## Git 状态

- **Commit**:`0095aba feat: Phase 1 第 3 章 - PromptTemplate + Advisor`
- **Push**:`137a30d..0095aba main -> main` ✅
- **Bundle**:`/Users/zhurenbao/Documents/spring-ai-2-0-projects-20260811-1752-chapter-3.bundle`

## 仓库 commit 历史(6 个)

```
0095aba feat: Phase 1 第 3 章 - PromptTemplate + Advisor
137a30d docs: 记录 Chapter 2 决策
ea2a8c4 feat: Phase 1 第 2 章 - ChatClient API 深入
6ffbc26 docs: 记录 W0 收尾 + Chapter 1 决策
002004f feat: W0 收尾 + Phase 1 第 1 章 Hello World
644a8de chore: 初始化仓库
```

## Phase 1 进度(3/6)

| # | 章节 | 状态 |
|---|---|---|
| 1 | Hello World | ✅ |
| 2 | ChatClient API | ✅ |
| 3 | Prompt + Advisor | ✅ |
| 4 | Function Calling | ⏳ |
| 5 | Structured Output | ⏳ |
| 6 | Streaming | ⏳ |

## 下一步

Phase 1 第 4 章:`04-function-calling` - LLM 调你的 Java 方法(@Tool 注解)
