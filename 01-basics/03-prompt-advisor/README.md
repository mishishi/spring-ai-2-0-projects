# 第 3 章 · Prompt + Advisor

> 🎯 目标:掌握 `PromptTemplate` 参数化 + `Advisor` 拦截器模式

## 你将学到

- ✅ `PromptTemplate` 用 `{var}` 占位符 + `.render(Map)` 渲染
- ✅ `defaultAdvisors(...)` 全局配置 advisor
- ✅ `SimpleLoggerAdvisor` 内置 advisor:自动打 LLM 请求/响应日志
- ✅ Advisor 模式 = 类比 Spring AOP,在 LLM 调用前后插入逻辑

## 快速开始

```bash
cd 01-basics/03-prompt-advisor
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

跑起来会看到:
- 2 个 demo(用 `══════` 分隔)
- `SimpleLoggerAdvisor` 自动打的 before/after 日志(看 console)

## 关键代码解读

### Demo 1:PromptTemplate 参数化

```java
String template = """
    你是一个 {role}。
    用户名字: {name}
    用户问题: {question}
    请用 {style} 风格回答。
    """;

PromptTemplate pt = new PromptTemplate(template);
String rendered = pt.render(Map.of(
    "role", "Java 架构师",
    "name", "Alice",
    "question", "Spring Boot 启动慢怎么办?",
    "style", "通俗易懂,带点幽默"));

String reply = client.prompt()
    .user(rendered)
    .call()
    .content();
```

**对比 1**:
```java
// 没模板(参数硬编码)
client.prompt().user("你是一个 Java 架构师。用户名字: Alice。问题: ...").call().content();

// 有模板(参数可复用)
PromptTemplate pt = new PromptTemplate("你是一个 {role}。用户名字: {name}。问题: {question}");
String input = pt.render(Map.of("role", "...", "name", "...", "question", "..."));
```

### Demo 2:Advisor 拦截器

```java
ChatClient client = builder
    .defaultAdvisors(new SimpleLoggerAdvisor())  // 全局生效
    .defaultSystem("...")
    .build();

// 之后所有 prompt() 都自动走 advisor
client.prompt().user("...").call().content();
```

`SimpleLoggerAdvisor` 自动在 LLM 调用前后打日志:
```
[SimpleLoggerAdvisor] before: ChatClientRequest{prompt=Prompt{messages=[UserMessage{content='...'}, ...]}, context={...}}
[SimpleLoggerAdvisor] after: ChatClientResponse{chatResponse=ChatResponse{...}}
```

**不用写任何 logger 代码,advisor 全自动打**。

## PromptTemplate API 速查

| 方法 | 作用 |
|---|---|
| `new PromptTemplate(String)` | 从字符串构造 |
| `new PromptTemplate(Resource)` | 从 classpath 资源构造(`.st` 文件) |
| `.render(Map<String, Object>)` | 渲染成 String |
| `.create(Map<String, Object>)` | 渲染成 `Prompt` 对象(包含 system + user) |
| `.createMessage(Map<String, Object>)` | 渲染成 `Message` |

`{var}` 占位符(花括号语法):
```java
new PromptTemplate("你好 {name},今天 {date}").render(Map.of(
    "name", "Alice",
    "date", "2026-08-11"));
```

## Advisor 模式深度理解

**类比 Spring AOP**:

| Spring AOP | Spring AI Advisor |
|---|---|
| `@Around` 切面 | `CallAdvisor` / `StreamAdvisor` |
| `MethodInvocation` | `ChatClientRequest` / `ChatClientResponse` |
| `proceed()` | `next.call()` / `next.stream()` |
| `getOrder()` | 顺序(order 越小越先) |
| `Filter` 链 | `defaultAdvisors(...)` |

**Advisor 链执行顺序**:
```
prompt() → Advisor1.before() → Advisor2.before() → LLM call
        → Advisor2.after()  → Advisor1.after()  → return
```

**3 个最常用的 Advisor 模式**:
1. **日志**(`SimpleLoggerAdvisor`)— 看请求/响应
2. **Memory**(`MessageChatMemoryAdvisor`)— 多轮对话(下章讲)
3. **RAG**(`QuestionAnswerAdvisor`)— 检索增强(Phase 2 讲)

## 内置 Advisor 列表(Spring AI 1.1.3)

| Advisor | 作用 | 何时用 |
|---|---|---|
| `SimpleLoggerAdvisor` | 打请求/响应日志 | 调试 / 排查 |
| `MessageChatMemoryAdvisor` | 多轮对话(messages 累积) | 聊天场景 |
| `PromptChatMemoryAdvisor` | 多轮对话(prompt 形式) | 聊天场景 |
| `QuestionAnswerAdvisor` | RAG 检索 | RAG 应用 |
| `SafeGuardAdvisor`(需自定义) | 敏感词拦截 / 脱敏 | 涉密场景 |

## 测试

```bash
mvn test
```

0 网络(跟 chapter 1/2 一样的策略)。

## 目录结构

```
03-prompt-advisor/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/promptadvisor/
    │   │   └── Application.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/cc/misshi/springai/promptadvisor/
            └── ApplicationTests.java
```

## 下一章

[第 4 章 · Function Calling →](../04-function-calling/README.md)

让 LLM **调用你的 Java 方法**(查询数据库 / 调外部 API / 算数学)
