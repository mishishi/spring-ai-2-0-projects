# 第 4 章 · Function Calling


## 你将学到

- ✅ `@Tool` 注解把 Java 方法暴露给 LLM
- ✅ `@ToolParam` 描述参数,LLM 才能选对
- ✅ `ChatClient.defaultTools(...)` 注册工具集
- ✅ 4 个实战场景:读 / 写 / 多工具 / 工具组合
- ✅ LLM 自动决定何时调 + 调哪个 + 传什么参数
- ✅ description 编写 4 原则(动词 / 格式 / 结构 / 边界)
- ✅ 错误处理 + 让 LLM 自动重试

## 一句话总结

`@Tool` 注解把 Java 方法暴露给 LLM,LLM 自动判断何时调、调哪个、调什么参数 — **不需要写 if-else 分发逻辑**。

## 读者学完能做什么

- 把任何 Java 方法变成 LLM 工具
- 让 LLM 查数据库 / 调 API / 算业务逻辑
- 理解 LLM 的 tool selection 机制
- 写出清晰的 `@ToolParam` description
- 处理 LLM 传错参数的边界情况
- 工具组合(LLM 调多个工具综合回答)

## 5 分钟上手

```bash
export OPENAI_API_KEY=sk-xxxxx
cd 01-basics/04-function-calling
mvn spring-boot:run
```

跑 3 个 demo:
1. 时间工具 — "现在几点了?"
2. 数学计算 — "23 + 45 = ?"
3. 工具组合 — LLM 同时调 2 个工具

## 为什么需要 Function Calling(背景)

在 LLM 出现之前,应用是这样的:

```java
// 老套路:正则 + if-else
if (input.contains("几点") || input.contains("时间")) {
    return new Date().toString();
} else if (input.matches(".*\\d+.*\\+.*\\d+.*")) {
    return calc(input);
}
```

痛点:
- **写死规则** — 100 种问题要写 100 个 if
- **不支持组合** — "我多大了"要查年份 + 算年龄,需要手工拼接
- **不灵活** — 用户说"现在几点几分",正则就跪

**Function Calling 解决的**:

```java
// 新套路:LLM 自己决定
@Tool(description = "Get current date and time")
public String getCurrentTime() { return ...; }

@Tool(description = "Calculate sum of two numbers")
public int add(int a, int b) { return a + b; }

client.prompt().user("我 2000 年出生,今年多大了?").call().content();
// LLM:1) 调 getCurrentYear() 拿 2026
//     2) 算 2026-2000 = 26
//     3) 回答 "26 岁"
```

**类比**:
- 老套路 = 命令式编程(你告诉程序每一步)
- Function Calling = 声明式编程(你告诉程序"能做啥",让 LLM 决定怎么做)

## 工作原理(LLM 视角)

```
1. 用户问: "现在几点了?"
2. Spring AI 序列化所有 @Tool 方法 → 工具列表(给 LLM 看)
3. LLM 看工具列表 + 用户问题 → 决定调 getCurrentTime()
4. Spring AI 调 Java 方法 → 拿返回 "2026-08-12 17:55"
5. LLM 拿结果 → 写自然语言回答
6. 返回 "现在是 2026-08-12 17:55"
```

**LLM 不直接执行代码,它只是"决定调什么 + 传什么参数"**。真正执行的是 Spring AI 框架。

## 关键概念(4 个)

### 概念 1:`@Tool` 注解

```java
@Tool(description = "Get the current date and time in yyyy-MM-dd HH:mm:ss format")
public String getCurrentTime() {
    return LocalDateTime.now().format(...);
}
```

**5 个可选参数**:

| 参数 | 作用 | 默认 |
|---|---|---|
| `description` | 工具描述(LLM 看的)**必填** | - |
| `name` | 工具名 | 方法名 |
| `returnDirect` | 是否直接返回不二次加工 | false |
| `resultConverter` | 结果转换器 | 默认 |
| `parameConverters` | 参数转换器 | 默认 |

### 概念 2:`@ToolParam`

```java
@Tool(description = "Search users by name")
public List<User> searchByName(
    @ToolParam(description = "Name keyword, fuzzy match, case-insensitive") String keyword,
    @ToolParam(description = "Max results, default 10, max 100") int limit
) { ... }
```

**`@ToolParam` 必填,否则 LLM 瞎传**:
- 没 `@ToolParam`:LLM 看到 `String` 不知道传啥格式
- 有 `@ToolParam`:LLM 看到 "yyyy-MM-dd format" 知道传日期字符串

### 概念 3:`Tools` 类(纯 POJO + 注解)

工具类**不需要继承 / 实现任何接口** — 任何 Java 类都能当工具集:

```java
public class TimeTools {                         // 不用 extends
    @Tool(description = "Get current time")      // 注解暴露
    public String getCurrentTime() { ... }
}

public class MathTools {
    @Tool(description = "Add two numbers")
    public int add(int a, int b) { return a + b; }
}
```

**Spring AI 通过反射扫**所有 `@Tool` 方法,自动注册。

### 概念 4:`ChatClient.defaultTools(...)`

把工具集挂到 ChatClient:

```java
ChatClient client = builder
    .defaultTools(new TimeTools(), new MathTools())
    .defaultSystem("你是助手,回答简短")
    .build();

// 现在这个 client 调 LLM 时,所有 user message 都会带工具列表
client.prompt().user("现在几点了?").call().content();
// LLM 看到工具列表,决定调 getCurrentTime
```

**作用域**:`defaultTools` 是 client 级别的,挂上就全程生效。也可以 per-call 覆盖:

```java
client.prompt()
    .user("...")
    .tools(new OtherTools())          // per-call 工具(覆盖 defaultTools)
    .call().content();
```

## 4 个实战场景

### 场景 1:简单 @Tool(时间)

```java
public class TimeTools {
    @Tool(description = "Get the current date and time in yyyy-MM-dd HH:mm:ss format")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "Get the current year as an integer")
    public int getCurrentYear() {
        return LocalDateTime.now().getYear();
    }
}
```

### 场景 2:@ToolParam 参数 + 数学工具

```java
public class MathTools {
    @Tool(description = "Add two integers, returns the sum")
    public int add(
            @ToolParam(description = "First integer") int a,
            @ToolParam(description = "Second integer") int b) {
        return a + b;
    }

    @Tool(description = "Calculate the square root of a positive number")
    public double sqrt(
            @ToolParam(description = "Positive number to take square root of") double x) {
        if (x < 0) throw new IllegalArgumentException("x must be non-negative, got " + x);
        return Math.sqrt(x);
    }
}
```

### 场景 3:工具组合(LLM 调 2 个)

```java
ChatClient client = builder.defaultTools(new TimeTools(), new MathTools()).build();

String r = client.prompt()
    .user("现在是 2026 年,我 2000 年出生,多大了?")
    .call().content();
// LLM 内部流程:
// 1) 调 getCurrentYear() → 2026
// 2) 算 2026 - 2000 = 26
// 3) 回答 "26 岁"
```

**关键**:LLM 看到 `getCurrentYear()` 工具,**自己**决定"我需要调它来算年龄"。

### 场景 4:写操作工具(慎用 `returnDirect`)

```java
public class UserTools {
    @Tool(description = "Create a new user, returns the created user with auto-generated id")
    public User createUser(
            @ToolParam(description = "User name, 2-50 chars") String name,
            @ToolParam(description = "User email, must be valid format") String email) {
        return userRepo.save(new User(name, email));
    }
}
```

**`returnDirect` 风险**:
- 默认 `false` — LLM 调完工具,会把结果二次加工成自然语言(比如"用户已成功创建,id=42")
- 设 `true` — 直接返回工具结果(不加工),写操作场景慎用,可能让 LLM 跳过二次校验

## description 编写 4 原则

1. **动词开头**:"Get..." / "Search..." / "Calculate..." / "Create..."
2. **关键参数格式**:"in yyyy-MM-dd format" / "as ISO 8601" / "positive integer"
3. **返回结构**:"returns List<User> with fields id, name, email"
4. **边界 / 异常**:"throws if not found" / "returns null if user doesn't exist"

**反例** ❌:
```java
@Tool(description = "Get time")           // 太短,LLM 不知道格式
public String time() { return ...; }
```

**正例** ✅:
```java
@Tool(description = "Get the current date and time in yyyy-MM-dd HH:mm:ss format, returns string")
public String getCurrentTime() { ... }
```

## 错误处理

LLM 传错参数 → Spring AI 抛异常 → 默认会让 LLM 重新尝试(3 次)。

**自定义错误**:

```java
@Tool(description = "Get user by id")
public User getById(@ToolParam(description = "User id, positive long") long id) {
    if (id <= 0) {
        throw new IllegalArgumentException("id must be positive, got " + id);
    }
    return userRepo.findById(id).orElse(null);
}
```

LLM 看到错误信息会自己修正(比如改成 `Math.abs(id)` 或重新问用户)。

**禁用重试**(写操作防重复):

```java
@Tool(description = "Create a new order")
public Order createOrder(OrderRequest req) {
    return orderService.create(req);   // 抛异常不重试,避免重复下单
}
```

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test
    void contextLoads() {
        // 0 网络(Tools 不会主动注册到 LLM)
        // 只有 ChatClient.prompt() 才会触发 tool serialization
    }
}
```

**要真测工具**:
- mock ChatClient,验证 tools() 被注册
- 或用 Spring AI 的 `ToolCallingManager` 单测

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| `@Tool` 写但 description 空 | LLM 永远不调 | 必填,写清楚 |
| `@ToolParam` 漏写 | LLM 瞎传参数(尤其是 date / enum) | 每个参数都标 |
| 工具太多(>20 个) | LLM 选择困难,token 多 | 拆成多个 ChatClient,按场景 |
| 方法返回复杂对象(嵌套) | LLM 序列化失败 | 保持 POJO 扁平,只暴露必要字段 |
| `returnDirect=true` 用在写操作上 | 不走 LLM 二次校验,直接执行 | 慎用,默认 false |
| 工具方法抛未捕获异常 | Spring AI 500 错误 | 用 try-catch + 自定义 exception |
| 工具跟 system prompt 矛盾 | LLM 困惑 | description 跟 system 风格统一 |
| 工具方法太长(>1s) | LLM 等待,体验差 | 异步化 / 缓存 / 拆小 |

## 实战部署清单

- [ ] export `OPENAI_API_KEY`
- [ ] 写 `Tools` 类(纯 POJO + `@Tool`)
- [ ] 每个工具方法写 `description`(动词 + 格式 + 边界)
- [ ] 每个参数写 `@ToolParam description`
- [ ] `ChatClient.defaultTools(...)` 注册
- [ ] 跑 `mvn spring-boot:run` 验证
- [ ] 测试 LLM 调错参数 — 看是不是自动重试
- [ ] 工具数控制在 20 个以内
- [ ] 监控工具调用频率(防止循环)
- [ ] 写操作工具加幂等性(防重试重复执行)

## 完整代码

[01-basics/04-function-calling/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/04-function-calling)

## 下一步

- [第 5 章 · Structured Output →](05-structured-output.md)— 把 LLM 输出转 POJO
- [第 14 章 · Tool Calling 深入 →](14-tool-calling.md)— Agent 场景的 @Tool 实战
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
