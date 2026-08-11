# 第 3 章 · Prompt + Advisor

> Phase 1 · 基础筑基
> 🎯 掌握 PromptTemplate 参数化 + Advisor 拦截器模式

## 一句话总结

`PromptTemplate` 用 `{var}` 占位符让 prompt 可复用,`Advisor` 是 LLM 调用的"切面",在请求前后插入逻辑(日志 / memory / RAG / 安全)。

## 读者学完能做什么

- 写参数化 prompt(用户输入可注入变量)
- 配全局 advisor(自动打日志 / 累计 memory)
- 理解 advisor 链的执行顺序
- 知道以后 RAG / Agent 怎么用 advisor 组合

## PromptTemplate 3 种用法

### 1. 字符串模板(最常用)

```java
PromptTemplate pt = new PromptTemplate("""
    你是一个 {role}。
    用户问题: {question}
    """);
String input = pt.render(Map.of("role", "Java 架构师", "question", "..."));
client.prompt().user(input).call().content();
```

### 2. Resource 模板(从 .st 文件读)

`src/main/resources/prompts/coder-review.st`:
```
你是一个 {language} 专家。
代码:
```
{code}
```
请审查。
```

```java
PromptTemplate pt = new PromptTemplate(resourceLoader.getResource("classpath:prompts/coder-review.st"));
String input = pt.render(Map.of("language", "Java", "code", userCode));
```

### 3. 直接 create 成 Prompt

```java
Prompt prompt = pt.create(Map.of("role", "...", "question", "..."));
// Prompt 包含 messages / options,直接传给 chatModel.call(prompt)
```

## Advisor 模式本质

**类比 Spring AOP**:

```
┌─────────────────┐
│ client.prompt() │
└────────┬────────┘
         ↓
┌─────────────────────┐
│ Advisor1.before()   │ ← 改 prompt / 加 context
└────────┬────────────┘
         ↓
┌─────────────────────┐
│ Advisor2.before()   │ ← 改 prompt / 加 context
└────────┬────────────┘
         ↓
┌─────────────────────┐
│ LLM call            │
└────────┬────────────┘
         ↓
┌─────────────────────┐
│ Advisor2.after()    │ ← 处理 response / 存 memory
└────────┬────────────┘
         ↓
┌─────────────────────┐
│ Advisor1.after()    │
└────────┬────────────┘
         ↓
┌─────────────────────┐
│ return content      │
└─────────────────────┘
```

## 自定义 Advisor 模板

```java
@Slf4j
public class TimingAdvisor implements CallAdvisor {
    @Override
    public ChatClientRequest before(ChatClientRequest request) {
        request.context().put("startTime", System.currentTimeMillis());
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response) {
        Long start = (Long) response.context().get("startTime");
        log.info("LLM 耗时: {}ms", System.currentTimeMillis() - start);
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;  // 越早越好
    }
}
```

## 实战模式

| 场景 | Advisor 链 |
|---|---|
| 调试 | `SimpleLoggerAdvisor` |
| 聊天 | `SimpleLoggerAdvisor` + `MessageChatMemoryAdvisor` |
| RAG | `QuestionAnswerAdvisor`(Phase 2) |
| 安全 | 自定义 `SafeGuardAdvisor`(敏感词拦截) |
| 可观测 | 自定义 `TimingAdvisor`(耗时统计)+ Micrometer |

## 完整代码

[01-basics/03-prompt-advisor/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/03-prompt-advisor)

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| `{var}` 名字跟 template 不一致 | 渲染时抛 NPE / 没替换 | template 写 `role`,Map 也写 `role`,大小写敏感 |
| 多个 advisor 没设 order | 执行顺序乱 | 每个 advisor 实现 `getOrder()`,数字小的先 |
| `defaultAdvisors` 跟 `advisors` 混用 | 不生效 | `defaultAdvisors` 是 builder 级别,`advisors` 是 call 级别 |

## 下一步

- [第 4 章 · Function Calling →](../01-basics/04-function-calling/README.md)
- 让 LLM 调你的 Java 方法
