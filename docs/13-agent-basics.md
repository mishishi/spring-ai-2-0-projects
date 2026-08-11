# 第 13 章 · Agent Basics


## 一句话总结

**Agent = ChatClient + @Tool + ToolCallbackProvider** — Spring AI 2.0 把 Java 方法当 LLM 工具用,模型自动判断要不要调、调哪个。

## 你将学到

- ✅ `ChatClient.defaultTools(...)` 把工具注入对话
- ✅ `@Tool` + `@ToolParam` 注解,Java 方法直接暴露给 LLM
- ✅ `MethodToolCallbackProvider` 自动扫描 + 注册
- ✅ Agent loop:用户问 → LLM 判断 → 调工具 → 拿到结果 → 整理自然语言
- ✅ 0 网络测试套路(直接 new service,传 null)

## 快速开始

```bash
cd 03-agent/13-agent-basics
mvn test                          # 0 网络 8 tests
mvn spring-boot:run               # 真实跑 (需要 OPENAI_API_KEY)

# 真实 LLM 调用示例
curl "http://localhost:8080/agent/ask?question=北京天气怎么样?"
curl "http://localhost:8080/agent/ask?question=123+456等于多少?"
```

## 关键 API

### 1. `@Tool` + `@ToolParam` 注解

```java
@Component
public class WeatherTools {
    @Tool(description = "查询指定城市的当前天气")
    public String getCurrentWeather(
            @ToolParam(description = "城市名,如 '北京'") String city) {
        return "北京:晴 25°C,微风";  // mock
    }
}

@Component
public class CalculatorTools {
    @Tool(description = "把两个数字相加")
    public double add(
            @ToolParam(description = "第一个数") double a,
            @ToolParam(description = "第二个数") double b) {
        return a + b;
    }
}
```

**核心要点**:
- `description` 决定 LLM 何时调这个工具,描述要**精确**别模糊
- `@ToolParam` 给每个参数加描述,模型才知道怎么传值
- 多个 `@Tool` 方法可以同 class 注册,Spring 自动扫描

### 2. `MethodToolCallbackProvider` 注册

```java
@Bean
public ToolCallbackProvider toolCallbackProvider(
        WeatherTools weatherTools,
        CalculatorTools calculatorTools) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(weatherTools, calculatorTools)
            .build();
}
```

Spring AI 启动时自动扫描这两个 bean 里所有 `@Tool` 注解的方法。

### 3. `ChatClient.defaultTools(...)` 注入

```java
@Bean
public ChatClient agentClient(ChatClient.Builder builder, ToolCallbackProvider tools) {
    return builder
            .defaultSystem("""
                    你是 helpful 助手。
                    优先使用可用工具,不要编造数据。
                    """)
            .defaultTools(tools)   // 关键:把工具喂给 ChatClient
            .build();
}
```

## 3 个 Demo

### Demo 1: 简单问答

```bash
curl "http://localhost:8080/agent/ask?question=你好"
```

LLM 收到 `你好`,判断不需要工具,直接回答。

### Demo 2: 工具调用(隐式)

```bash
curl "http://localhost:8080/agent/ask?question=北京天气"
```

背后发生的事:
1. LLM 收到 "北京天气"
2. LLM 判断:有 `getCurrentWeather` 工具,需要 city="北京"
3. Spring AI 自动调 `weatherTools.getCurrentWeather("北京")`
4. 工具返回 "北京:晴 25°C"
5. LLM 整理成自然语言:"北京今天晴,25°C,微风"

**整个过程 0 行 Java 代码** — Spring AI 全包了。

### Demo 3: 多工具自动选择

```bash
curl "http://localhost:8080/agent/ask?question=计算 (123+456)*2"
```

LLM 看到 `计算`,自动选 `add` 工具,参数 123 + 456 → 579,再 * 2 → 1158,返回。

## 踩坑(Agent 基础 3 大坑)

### 坑 1: `description` 太模糊,模型不调

```java
// ❌ Bad: 描述太宽泛
@Tool(description = "查询")
public String query(String q) { ... }

// ✅ Good: 描述具体场景
@Tool(description = "查询指定城市的当前天气,只支持中国城市名")
public String getCurrentWeather(String city) { ... }
```

### 坑 2: 返回类型不支持

```java
// ❌ Bad: 返回自定义对象但没 toString / 非 record
@Tool
public MyCustomObject getData() { ... }

// ✅ Good: 返回 String / record / Map<String,Object>
@Tool
public String getData() { ... }
```

Spring AI 自动把 record 序列化成 JSON,模型能直接读。

### 坑 3: 参数是基本类型没 @ToolParam

```java
// ❌ Bad: 没描述,模型不知道传什么
@Tool
public String search(String keyword, int page) { ... }

// ✅ Good: 每个参数加 description
@Tool
public String search(
        @ToolParam(description = "搜索关键词") String keyword,
        @ToolParam(description = "页码,从 1 开始") int page) { ... }
```

## 0 网络测试套路

```java
// 直接 new CalculatorTools,不需要 Spring 容器
CalculatorTools tools = new CalculatorTools();
double sum = tools.add(2, 3);
assertThat(sum).isEqualTo(5.0);
```

8 tests 全过,无 LLM 调用,无网络。

## 实战清单

- [x] `@Tool` + `@ToolParam` 注解
- [x] `MethodToolCallbackProvider` 自动扫描
- [x] `ChatClient.defaultTools()` 注入
- [ ] **生产补 1**:工具调用限流(防 LLM 死循环调工具)— 章节 18
- [ ] **生产补 2**:多轮对话记忆 — 章节 18
- [ ] **生产补 3**:MCP 协议 — 章节 15

## 完整代码

[03-agent/13-agent-basics/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/03-agent/13-agent-basics)

## 学完下一步

[14 Tool Calling 进阶 →](14-tool-calling.md) — 5 大特性(`returnDirect` / `required` / `ToolContext` / POJO / `FunctionToolCallback`)
