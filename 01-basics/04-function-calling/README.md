# 第 4 章 · Function Calling

> 🎯 目标:让 LLM **调用你的 Java 方法**(查数据库 / 调 API / 算数学)

## 你将学到

- ✅ `@Tool` 注解:把 Java 方法暴露给 LLM
- ✅ `@ToolParam` 注解:给参数加 description
- ✅ `defaultTools(...)`:全局注册工具
- ✅ LLM 自动决定调哪个工具 / 传什么参数

## 快速开始

```bash
cd 01-basics/04-function-calling
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

3 个 demo 顺序跑(用 `══════` 分隔):

```
══════ Demo 1: 时间工具 ══════
[LLM 自动调 TimeTools.getCurrentTime()]
🤖 现在是 2026-08-11 17:55,今天是 11 号

══════ Demo 2: 数学计算 ══════
[LLM 自动调 MathTools.add(23, 45)]
🤖 23 + 45 = 68

══════ Demo 3: 工具组合 ══════
[LLM 自动调 TimeTools.getCurrentYear() + 算减法]
🤖 你是 2000 年出生,现在是 2026 年,所以 26 岁
```

## 关键代码

### 1. 定义工具(@Tool + @ToolParam)

```java
public class TimeTools {

    @Tool(description = "Get the current date and time in yyyy-MM-dd HH:mm:ss format")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

public class MathTools {

    @Tool(description = "Add two numbers and return the sum")
    public int add(
            @ToolParam(description = "The first number") int a,
            @ToolParam(description = "The second number") int b) {
        return a + b;
    }
}
```

### 2. 注册工具(defaultTools)

```java
ChatClient client = builder
    .defaultTools(new TimeTools(), new MathTools())
    .build();

// 之后 LLM 自动发现 + 决定调
String reply = client.prompt().user("现在几点了?").call().content();
// LLM 自动调 getCurrentTime() 然后回答
```

## @Tool 注解规范

| 元素 | 必填 | 说明 |
|---|---|---|
| `description` | ✅ | **关键**:LLM 靠这个决定何时调、调什么。写不清楚,LLM 永远不调 |
| `name` | ❌ | 默认用方法名(`getCurrentTime`),可以显式覆盖 |
| `returnDirect` | ❌ | true = 直接把方法返回值给用户,不走 LLM 二次生成 |

**description 模板**:
```
"动词 + 对象 + 关键信息"
```

**好 vs 坏**:

| ❌ Bad | ✅ Good |
|---|---|
| "Get time" | "Get the current date and time in yyyy-MM-dd HH:mm:ss format" |
| "Add" | "Add two integers and return the sum" |
| "Query user" | "Query user info by user id, returns name/age/email or null if not found" |

## @ToolParam 注解规范

```java
@Tool(description = "Search flights")
public List<Flight> searchFlights(
    @ToolParam(description = "Departure city, e.g. Beijing") String from,
    @ToolParam(description = "Arrival city, e.g. Shanghai") String to,
    @ToolParam(description = "Departure date in yyyy-MM-dd format") String date) {
    // ...
}
```

**规则**:
- ✅ 每个参数都要 `@ToolParam`
- ✅ description 写示例(`e.g. Beijing`)
- ✅ 格式写明(`yyyy-MM-dd`)
- ❌ 不要写"用户输入"这种废话

## 3 种工具注册方式

### 1. `defaultTools(...)`(全局)— 推荐

```java
ChatClient client = builder
    .defaultTools(new TimeTools(), new MathTools())
    .build();
```

### 2. `prompt().tools(...)`(per-call)

```java
String reply = client.prompt()
    .tools(new DatabaseTools())  // 这次只让 LLM 用这个
    .user("查 user_id=123 的信息")
    .call().content();
```

### 3. Function bean(动态注册)

```java
@Bean
@Description("Get current weather for a city")
public Function<WeatherRequest, WeatherResponse> weatherFunction() {
    return req -> weatherService.getCurrent(req.city());
}
```

Spring AI 自动发现 `@Bean Function<>`。

## 实战模式

| 场景 | 工具 |
|---|---|
| 查数据库 | `@Tool` 包 `userRepository.findById(id)` |
| 调外部 API | `@Tool` 包 `weatherClient.get(city)` |
| 算数学 | `@Tool` 包 `calculator.add(a, b)` |
| 算业务逻辑 | `@Tool` 包 `pricingEngine.calc(items)` |
| 触发动作 | `@Tool` 包 `emailService.send(to, subject, body)` |

## 测试

```bash
mvn test
```

0 网络(测试 profile 不跑 demo)。

## 目录结构

```
04-function-calling/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/functioncalling/
    │   │   ├── Application.java
    │   │   ├── TimeTools.java
    │   │   └── MathTools.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/cc/misshi/springai/functioncalling/
            └── ApplicationTests.java
```

## 下一章

[第 5 章 · Structured Output →](../05-structured-output/README.md)

把 LLM 输出从 String 转成 Java POJO,强类型!
