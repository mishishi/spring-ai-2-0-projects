# 第 4 章 · Function Calling


## 一句话总结

`@Tool` 注解把 Java 方法暴露给 LLM,LLM 自动判断何时调、调哪个、调什么参数 — **不需要写 if-else 分发逻辑**。

## 读者学完能做什么

- 把任何 Java 方法变成 LLM 工具
- 让 LLM 查数据库 / 调 API / 算业务逻辑
- 理解 LLM 的 tool selection 机制
- 写出清晰的 `@ToolParam` description

## 工作原理(LLM 视角)

```
1. 用户问: "现在几点了?"
2. Spring AI 序列化所有 @Tool 方法 → 工具列表
3. LLM 看工具列表 + 用户问题 → 决定调 getCurrentTime()
4. Spring AI 调 Java 方法 → 拿返回 "2026-08-11 17:55"
5. LLM 拿结果 → 写自然语言回答
6. 返回 "现在是 2026-08-11 17:55"
```

**LLM 不直接执行代码,它只是"决定调什么 + 传什么参数"**。真正执行的是 Spring AI 框架。

## @Tool 注解 5 个要素

```java
@Tool(
    description = "...",           // 必填
    name = "...",                  // 可选,默认方法名
    returnDirect = false,          // 可选
    resultConverter = MyConv.class  // 可选
)
```

## description 编写 3 原则

1. **动词开头**:"Get..." / "Search..." / "Calculate..."
2. **关键参数格式**:"in yyyy-MM-dd format" / "as ISO 8601"
3. **返回结构**:"returns List<Flight> with fields..."

## 实战:用户管理工具集

```java
public class UserTools {
    private final UserRepository repo;
    public UserTools(UserRepository repo) { this.repo = repo; }

    @Tool(description = "Query user by id, returns User {id, name, email, age} or null if not found")
    public User getById(@ToolParam(description = "User id, must be positive long") long id) {
        return repo.findById(id).orElse(null);
    }

    @Tool(description = "Search users by name (fuzzy match), returns up to 10 users")
    public List<User> searchByName(@ToolParam(description = "Name keyword") String keyword) {
        return repo.findByNameContainingIgnoreCase(keyword, PageRequest.of(0, 10));
    }

    @Tool(description = "Create a new user, returns the created user with auto-generated id")
    public User create(
            @ToolParam(description = "User name, 2-50 chars") String name,
            @ToolParam(description = "User email, must be valid format") String email,
            @ToolParam(description = "User age, 0-150") int age) {
        return repo.save(new User(name, email, age));
    }
}
```

## 错误处理

LLM 传错参数 → Spring AI 抛异常 → 默认会让 LLM 重新尝试(3 次)。

**自定义错误**:
```java
@Tool(description = "...")
public User getById(long id) {
    if (id <= 0) {
        throw new IllegalArgumentException("id must be positive, got " + id);
    }
    return repo.findById(id).orElse(null);
}
```

LLM 看到错误信息会自己修正(比如改成 `Math.abs(id)` 或重新问用户)。

## 完整代码

[01-basics/04-function-calling/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/04-function-calling)

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| `@Tool` 写但 description 空 | LLM 永远不调 | 必填,写清楚 |
| `@ToolParam` 漏写 | LLM 瞎传参数(尤其是 date / enum) | 每个参数都标 |
| 工具太多(>20 个) | LLM 选择困难,token 多 | 拆成多个 ChatClient,按场景 |
| 方法返回复杂对象(嵌套) | LLM 序列化失败 | 保持 POJO 扁平 |
| `returnDirect=true` 用在写操作上 | 不走 LLM 二次校验,直接执行 | 慎用,默认 false |

## 下一步

- [第 5 章 · Structured Output →](05-structured-output.md)
- 把 LLM 的字符串输出转成强类型 Java 对象
