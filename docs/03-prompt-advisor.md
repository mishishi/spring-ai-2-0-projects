# 第 3 章 · Prompt + Advisor


## 你将学到

- ✅ `PromptTemplate` 用 `{var}` 占位符让 prompt 可复用
- ✅ 4 种用法:字符串 / Resource (.st 文件) / create Prompt / Map 渲染
- ✅ `Advisor` 是 LLM 调用的"切面",在请求前后插入逻辑
- ✅ Advisor 链执行顺序(`getOrder()` 控制)
- ✅ 4 个实战 Advisor:Logging / Timing / Memory / RAG 集成
- ✅ 写自定义 Advisor(实现 `CallAdvisor` 接口)

## 一句话总结

`PromptTemplate` 用 `{var}` 占位符让 prompt 可复用,`Advisor` 是 LLM 调用的"切面",在请求前后插入逻辑(日志 / memory / RAG / 安全)。

## 读者学完能做什么

- 写参数化 prompt(用户输入可注入变量)
- 配全局 advisor(自动打日志 / 累计 memory)
- 理解 advisor 链的执行顺序
- 写自定义 Advisor(实现 CallAdvisor)
- 把 RAG / Memory / Logging 全部用 advisor 组合
- 调 Advisor 顺序(getOrder)

## 5 分钟上手

```bash
export OPENAI_API_KEY=sk-xxxxx
cd 01-basics/03-prompt-advisor
mvn spring-boot:run
```

跑 3 个 demo:
1. PromptTemplate 字符串 / Resource 两种用法
2. Advisor 链(Logging + Timing)
3. 自定义 SafeGuardAdvisor(敏感词拦截)

## 为什么需要 PromptTemplate + Advisor(背景)

**痛点 1:prompt 字符串拼接混乱**

```java
// 老套路
String prompt = "你是一个" + role + ",用户问:" + question + "。";
// 引号 / 换行 / 变量顺序,容易出错
```

**PromptTemplate 解法**:
```java
// 模板 + 变量,关注分离
String tpl = "你是一个 {role}。用户问: {question}";
PromptTemplate pt = new PromptTemplate(tpl);
String input = pt.render(Map.of("role", "Java 架构师", "question", q));
```

**痛点 2:每个调用都要手动加日志 / 计时 / 缓存**

```java
// 老套路:每个 ChatClient.prompt() 前手动加
long start = System.currentTimeMillis();
log.info("开始调 LLM");
String r = client.prompt().user(q).call().content();
log.info("耗时:{}", System.currentTimeMillis() - start);
```

**Advisor 解法**:
```java
// 全局配一次,所有 client 自动生效
builder.defaultAdvisors(new SimpleLoggerAdvisor(), new TimingAdvisor());
// 所有 LLM 调用都自动计时 + 记日志
```

**类比**:
- PromptTemplate = JSP 模板引擎(数据 + 模板分离)
- Advisor = Spring AOP(横切关注点)

## 关键概念(4 个)

### 概念 1:`PromptTemplate` 渲染

```java
PromptTemplate pt = new PromptTemplate("""
    你是一个 {role}。
    用户问题: {question}
    """);
String input = pt.render(Map.of("role", "Java 架构师", "question", "..."));
```

**核心机制**:
- 模板字符串用 `{var}` 占位符
- `.render(Map)` 替换占位符
- 变量名必须跟 `{var}` 一致,**大小写敏感**
- 没传变量 → 占位符原样保留(运行时 LLM 看到 "请填 {question}")

### 概念 2:`Resource` 模板(.st 文件)

**适合**:prompt 比较长(> 20 行),从 .st 文件读。

`src/main/resources/prompts/coder-review.st`:
```
你是一个 {language} 专家。
代码:
```
{code}
```
请审查,返回:
- 是否有 bug
- 性能问题
- 安全漏洞
```

```java
@Value("classpath:prompts/coder-review.st") Resource tplResource;

PromptTemplate pt = new PromptTemplate(tplResource);
String input = pt.render(Map.of("language", "Java", "code", userCode));
```

**`.st` 命名**:Spring AI 约定(st = Spring Template),不强制。

### 概念 3:`pt.create(Map)` 直接成 Prompt

```java
Prompt prompt = pt.create(Map.of("role", "...", "question", "..."));
// Prompt 包含 messages / options,直接传给 chatModel.call(prompt)
```

**适用**:用 `ChatModel` 而不是 `ChatClient` 的场景(老 API)。

### 概念 4:Advisor 切面

**类比 Spring AOP**:

```
client.prompt().user(q).call().content()
    ↓
[Advisor1.before()]  ← 改 prompt / 加 context
    ↓
[Advisor2.before()]  ← 改 prompt / 加 context
    ↓
LLM call
    ↓
[Advisor2.after()]   ← 处理 response / 存 memory
    ↓
[Advisor1.after()]
    ↓
return content
```

**两个关键**:
- `before` 改 prompt(加 system / 改 user)
- `after` 处理 response(过滤 / 累加)

## 4 种 PromptTemplate 用法

### 用法 1:字符串模板(最常用)

```java
PromptTemplate pt = new PromptTemplate("""
    你是一个 {role}。用户问题: {question}
    """);
String input = pt.render(Map.of("role", "Java 架构师", "question", "..."));
client.prompt().user(input).call().content();
```

### 用法 2:Resource 模板

```java
@Value("classpath:prompts/coder-review.st") Resource tplResource;
PromptTemplate pt = new PromptTemplate(tplResource);
String input = pt.render(Map.of("language", "Java", "code", userCode));
```

### 用法 3:`.create()` 成 Prompt 对象

```java
Prompt prompt = pt.create(Map.of("role", "...", "question", "..."));
// Prompt 是 Spring AI 的统一 prompt 类型
```

### 用法 4:`.render()` + .stream()

```java
Flux<String> stream = client.prompt()
    .user(pt.render(Map.of("role", "...", "question", "...")))
    .stream()
    .content();
```

## Advisor 模式本质(完整流程)

```
┌─────────────────┐
│ client.prompt() │
└────────┬────────┘
         ↓
[Advisor1.before()]  ← order=10
   - 改 prompt
   - 加 context
         ↓
[Advisor2.before()]  ← order=20
   - retrieve docs
   - 拼进 system
         ↓
LLM call
         ↓
[Advisor2.after()]
   - 处理 response
   - 存 memory
         ↓
[Advisor1.after()]
   - 累加 metrics
         ↓
return content
```

**执行顺序**:`before` 升序(order 小先),`after` 降序(order 大先)。

## 4 个实战 Advisor

### 1. SimpleLoggerAdvisor(框架自带)

```java
ChatClient client = builder
    .defaultAdvisors(new SimpleLoggerAdvisor())
    .build();
// 所有 LLM 调用自动打印 prompt + response(DEBUG 级别)
```

### 2. 自定义 TimingAdvisor(耗时统计)

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
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
```

### 3. MessageChatMemoryAdvisor(自动累加对话)

```java
// 自动把"对话历史"拼进 prompt,LLM 看到完整上下文
ChatClient client = builder
    .defaultAdvisors(MessageChatMemoryAdvisor.builder()
        .chatMemoryRepository(new InMemoryChatMemoryRepository())
        .build())
    .build();
```

### 4. 自定义 SafeGuardAdvisor(敏感词拦截)

```java
@Slf4j
public class SafeGuardAdvisor implements CallAdvisor {
    private static final List<String> BLOCKED = List.of("password", "secret");

    @Override
    public ChatClientRequest before(ChatClientRequest request) {
        String userInput = request.prompt().getUserMessage().getText();
        for (String word : BLOCKED) {
            if (userInput.toLowerCase().contains(word)) {
                throw new IllegalArgumentException("敏感词拦截: " + word);
            }
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response) {
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;  // 最早拦截
    }
}
```

## Advisor 顺序控制

`getOrder()` 决定执行顺序:

```java
public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 100;  // 越小越先
}
```

**Spring 内置顺序**:

| Advisor | Order | 说明 |
|---|---|---|
| `SafeGuardAdvisor` | `HIGHEST_PRECEDENCE` | 最早拦截 |
| `MessageChatMemoryAdvisor` | `LOWEST_PRECEDENCE - 100` | 几乎最后 |
| `QuestionAnswerAdvisor` | 0 | 中间(跟 LLM 一起) |
| `SimpleLoggerAdvisor` | 0 | 中间 |

**自定义顺序**:
```java
new TimingAdvisor();              // 最早(算总耗时)
new SafeGuardAdvisor();            // 第二(拦截)
new QuestionAnswerAdvisor(...);   // 第三(拼 context)
new SimpleLoggerAdvisor();         // 第四(打印)
new MessageChatMemoryAdvisor(...); // 最晚(存 history)
```

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test
    void contextLoads() {
        // 0 网络(advisor lazy 加载)
    }
}
```

**要真测 Advisor**:
- 测 `before` 改写 prompt
- 测 `after` 处理 response
- 测 `getOrder` 顺序

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| `{var}` 名字跟 Map 不一致 | 渲染时占位符没替换 | template 写 `role`,Map 也写 `role`,大小写敏感 |
| 多个 advisor 没设 order | 执行顺序乱 | 每个 advisor 实现 `getOrder()`,数字小的先 |
| `defaultAdvisors` 跟 `advisors` 混用 | 不生效 | `defaultAdvisors` 是 builder 级别,`advisors` 是 call 级别 |
| Advisor 抛异常没捕获 | LLM 整调用挂 | 加 try-catch,fallback 友好提示 |
| `.st` 文件编码错 | 中文乱码 | UTF-8,IDE 默认 |
| 模板里写 `${var}` 跟 Spring 占位符冲突 | 启动报错 | 改 `{var}` 单花括号 |
| Prompt 模板太长(>2000 字) | 浪费 token | 拆分到多个小模板,组合 |
| Advisor 里改 prompt 没效果 | LLM 没看到 | 确认 `before` return 了改过的 request |

## 实战部署清单

- [ ] 写常用 prompt 用 `PromptTemplate`(不用字符串拼接)
- [ ] 模板 .st 文件放 `resources/prompts/`
- [ ] 默认挂 `SimpleLoggerAdvisor`(开发期)
- [ ] 写 `TimingAdvisor`(统计 LLM 耗时)
- [ ] 写 `SafeGuardAdvisor`(敏感词拦截)
- [ ] RAG 场景用 `QuestionAnswerAdvisor`
- [ ] 聊天场景用 `MessageChatMemoryAdvisor`
- [ ] 调 `getOrder()` 控制执行顺序
- [ ] `mvn test` 0 网络 PASS

## 完整代码

[01-basics/03-prompt-advisor/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/03-prompt-advisor)

## 下一步

- [第 4 章 · Function Calling →](04-function-calling.md)— 让 LLM 调 Java 方法
- [第 5 章 · Structured Output →](05-structured-output.md)— 把 LLM 输出转 POJO
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
