# 第 5 章 · Structured Output


## 一句话总结

`.entity(Class)` / `.entity(ParameterizedTypeReference)` 一步把 LLM 输出转成 POJO,Spring AI 用 Jackson 帮你解析 JSON,不用手写 `ObjectMapper.readValue()`。

## 读者学完能做什么

- 用 record 写 LLM 输出的 POJO
- 单 POJO 提取(`.entity(Class)`)
- 列表提取(`.entity(ParameterizedTypeReference)`)
- 处理嵌套对象
- 用 Jackson 注解控制 LLM 输出格式

## 3 种输出形式

| 形式 | 代码 | 适用 |
|---|---|---|
| String | `.content()` | 简单聊天 |
| POJO | `.entity(Class)` | 信息提取 / 表单 |
| 泛型 | `.entity(TypeReference)` | 列表 / 嵌套 |

## 工作原理

```
1. Spring AI 根据 POJO 生成 JSON Schema
2. LLM 按 schema 输出(JSON mode)
3. Spring AI 用 Jackson 反序列化 → POJO
4. 返回强类型对象
```

**关键**:Spring AI 在 prompt 里注入 JSON Schema,LLM 知道要输出什么结构。

## ParameterizedTypeReference 3 种写法

```java
// 1. 匿名类(常用)
new ParameterizedTypeReference<List<Movie>>() {}

// 2. TypeReference 静态方法
TypeReference.forType(
    new TypeReference<List<Movie>>() {}.getType())

// 3. Java 反射(不推荐)
((ParameterizedType) getClass()
    .getGenericSuperclass())
    .getActualTypeArguments()[0]
```

## 实战:NER 信息提取

```java
public record Person(String name, int age, String occupation) {}

Person p = client.prompt()
    .user("提取人物: 'Bob,30 岁,Java 工程师'")
    .call()
    .entity(Person.class);
```

## 实战:产品评论分类

```java
public enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

public record Review(String text, Sentiment sentiment) {}

Review r = client.prompt()
    .user("""
        评论: "这个手机太烂了,千万别买!"
        判断情感(positive/negative/neutral)
        """)
    .call()
    .entity(Review.class);
```

## 完整代码

[01-basics/05-structured-output/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/05-structured-output)

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| LLM 输出不符合 schema | 抛 `JsonParseException` | 降 temperature / 加 `format` 指令 |
| 嵌套对象 LLM 不会填 | 字段为 null | 简化 schema / 拆分调用 |
| 日期类型 | 反序列化失败 | 用 `LocalDate` 配 `@JsonFormat` |
| `enum` 值不在定义里 | 抛异常 | 改用 `String` 接受 + 手动转换 |

## 下一步

- [第 6 章 · Streaming →](06-streaming.md)
- WebFlux + SSE 实时推流到浏览器
