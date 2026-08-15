# 第 14 章 · Tool Calling 进阶


## 一句话总结

**5 大特性搞定 90% 工具调用场景** — `returnDirect` / `required=false` / `ToolContext` / POJO 复杂参数 / `FunctionToolCallback` 函数式工具。

## 你将学到

- ✅ `returnDirect=true` 让工具结果直接返回,不再让 LLM 加工
- ✅ `@ToolParam(required = false)` 可选参数
- ✅ `ToolContext` 注入租户 ID / 用户 ID 等上下文
- ✅ POJO 复杂参数(自动生成 JSON Schema)
- ✅ `FunctionToolCallback` 函数式工具(无 @Tool 注解)

## 快速开始

```bash
cd 03-agent/14-tool-calling
mvn test                          # 0 网络 7 tests
mvn spring-boot:run

curl "http://localhost:8080/tool/ask?question=查询客户ID为1的信息"
curl "http://localhost:8080/tool/ask-with-context?question=列出我所有客户&tenantId=acme"
```

## 关键 API(5 大特性逐一拆解)

### 特性 1: `returnDirect` — 工具结果直返

```java
@Tool(description = "查询客户信息,直接返回结构化数据", returnDirect = true)
public Customer getCustomerInfo(
        @ToolParam(description = "客户 ID,正整数") Long id) {
    return findById(id);
}
```

**何时用**: 工具返回的就是最终答案,不需要 LLM 再加工。
- ✅ 数据库查询结果
- ✅ API 调用结果
- ❌ 不适合"工具 + 自然语言解释"的场景

### 特性 2: `required = false` 可选参数

```java
@Tool(description = "更新客户信息,email 是可选的")
public String updateCustomerInfo(
        @ToolParam(description = "客户 ID") Long id,
        @ToolParam(description = "新客户名称") String name,
        @ToolParam(description = "新邮箱(可选)", required = false) String email) {
    if (email == null || email.isBlank()) {
        return String.format("已更新客户 %d 名称为 '%s'(邮箱未改)", id, name);
    }
    return String.format("已更新客户 %d: 名称='%s', 邮箱='%s'", id, name, email);
}
```

**何时用**: 参数有合理默认值或可以不传。

### 特性 3: `ToolContext` 上下文注入

```java
@Tool(description = "查询当前租户的所有客户(自动从 ToolContext 拿 tenantId)")
public List<Customer> listMyCustomers(ToolContext toolContext) {
    String tenantId = (String) toolContext.getContext().get("tenantId");
    return List.of(
        new Customer(1L, "Alice (" + tenantId + ")", "alice@" + tenantId + ".com"),
        new Customer(2L, "Bob (" + tenantId + ")", "bob@" + tenantId + ".com")
    );
}
```

调用方传 context:
```java
chatClient.prompt()
    .user(question)
    .toolContext(Map.of("tenantId", "acme"))   // 注入
    .call()
    .content();
```

**何时用**:
- 多租户 SaaS(每个租户只看自己数据)
- 权限隔离(用户 ID / 角色)
- 请求追踪(traceId)

### 特性 4: POJO 复杂参数

```java
@Tool(description = "创建订单,需要客户 ID + 至少一个商品")
public String createOrder(CreateOrderRequest request) {
    return String.format("订单已创建:客户=%d, 商品数=%d, 总价=%.2f",
            request.customerId(), request.items().size(), request.totalAmount());
}

public record CreateOrderRequest(Long customerId, List<OrderItem> items, double totalAmount) {}
public record OrderItem(String sku, int quantity, double price) {}
```

Spring AI 自动从 record 生成 JSON Schema,模型可以传嵌套对象:
```json
{
  "customerId": 1,
  "items": [{"sku": "A001", "quantity": 2, "price": 99.0}],
  "totalAmount": 198.0
}
```

### 特性 5: `FunctionToolCallback` 函数式工具

```java
@Bean
public ToolCallback dateTools() {
    return FunctionToolCallback.builder("currentDate", (Function<EmptyRequest, String>) req -> LocalDate.now().toString())
            .description("查询当前日期,返回 YYYY-MM-DD")
            .inputType(EmptyRequest.class)
            .build();
}
```

**何时用**:
- 工具逻辑简单(几行 lambda)
- 不需要建 class(Java 17 函数式编程)
- 想快速验证一个 tool idea

## 3 个 Demo

### Demo 1: `returnDirect` 直查客户

```bash
curl "http://localhost:8080/tool/ask?question=查客户ID 1 的信息"
```

模型看到查询需求,直接调 `getCustomerInfo(1)`,返回 `Customer(1, "Customer 1", "...")` 序列化,LLM 看到 JSON 不再加工(因为 returnDirect=true)。

### Demo 2: `ToolContext` 多租户

```bash
curl "http://localhost:8080/tool/ask-with-context?question=列出我所有客户&tenantId=acme"
```

背后:`toolContext({tenantId: "acme"})` → `listMyCustomers` 拿 tenantId="acme" → 返回带 tenant 标签的 mock 数据。

### Demo 3: POJO 嵌套参数

LLM 拼一个 `CreateOrderRequest` JSON:
```json
{"customerId": 1, "items": [{"sku": "A", "quantity": 3, "price": 50}], "totalAmount": 150}
```
Spring AI 自动反序列化成 record,工具收到完整对象。

## 踩坑(3 大常见)

### 坑 1: `required = false` 但没 null check

```java
// ❌ NPE
@Tool
public String update(@ToolParam(required = false) String email) {
    return email.toLowerCase();   // email 可能 null
}

// ✅ null safe
if (email == null || email.isBlank()) { ... }
```

### 坑 2: `ToolContext` 类型强转 ClassCastException

```java
// ❌ 假设一定是 String
String tenantId = (String) toolContext.getContext().get("tenantId");

// ✅ 安全转换
Object val = toolContext.getContext().get("tenantId");
if (val instanceof String s) { ... }
```

### 坑 3: POJO 嵌套 record 没写 record

```java
// ❌ 普通 class,Spring AI 反射不友好
public static class OrderItem { ... }

// ✅ record
public record OrderItem(String sku, int quantity, double price) {}
```

## 0 网络测试

7 tests,直接 `new CustomerTools()` 测试每个工具方法,验证参数解析和返回值。

## 实战清单

- [x] `returnDirect` 让工具直返
- [x] `required = false` 可选参数
- [x] `ToolContext` 注入上下文
- [x] POJO record 嵌套
- [x] `FunctionToolCallback` 函数式
- [ ] **生产补 1**:工具调用 retry(网络抖动)— 章节 18
- [ ] **生产补 2**:Tool 调用链追踪(traceId)— 章节 18

## 完整代码

[03-agent/14-tool-calling/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/03-agent/14-tool-calling)

## 学完下一步

[15 MCP →](16-mcp.md) — Anthropic Model Context Protocol,把工具暴露给**任何** MCP 客户端(Claude Desktop / 其他 AI 应用)。
