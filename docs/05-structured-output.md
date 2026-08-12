# 第 5 章 · Structured Output


## 你将学到

- ✅ `.entity(Class)` 一行把 LLM 输出转 POJO
- ✅ `.entity(ParameterizedTypeReference)` 处理 List / 嵌套
- ✅ record 当 POJO(Java 17 现代写法)
- ✅ Jackson 注解控制 LLM 输出格式(`@JsonProperty` / `@JsonFormat`)
- ✅ enum / 嵌套对象 / 数组 4 个实战场景
- ✅ LLM 不合规输出的兜底策略

## 一句话总结

`.entity(Class)` / `.entity(ParameterizedTypeReference)` 一步把 LLM 输出转成 POJO,Spring AI 用 Jackson 帮你解析 JSON,不用手写 `ObjectMapper.readValue()`。

## 读者学完能做什么

- 用 record 写 LLM 输出的 POJO
- 单 POJO 提取(`.entity(Class)`)
- 列表提取(`.entity(ParameterizedTypeReference)`)
- 处理嵌套对象 + enum
- 用 Jackson 注解控制 LLM 输出格式
- 处理 LLM 输出不合规的兜底

## 5 分钟上手

```bash
export OPENAI_API_KEY=sk-xxxxx
cd 01-basics/05-structured-output
mvn spring-boot:run
```

跑 2 个 demo:
1. 单 POJO — 从 "Bob,30 岁,Java 工程师" 提取 Person
2. POJO 列表 — 推荐 3 部科幻电影 → List<Movie>

## 为什么需要 Structured Output(背景)

LLM 默认输出是 **String**。在生产里 80% 的场景需要结构化:

```java
// 现状(差)
String response = client.prompt().user("提取人物").call().content();
// "Bob 是一个 30 岁的 Java 工程师"
// 字符串,没法直接入库 / 调 API / 渲染 UI

// 期望(好)
Person person = client.prompt().user("提取人物").call().entity(Person.class);
// person.name = "Bob", person.age = 30, person.occupation = "Java 工程师"
```

**手动解析的问题**:
- LLM 输出可能带 "好的,以下是..." 等废话,要正则过滤
- JSON 偶尔有错(尾逗号 / 多余字段)
- 字段名要自己映射(LLM 输出 snake_case,Java 是 camelCase)

**Structured Output 解决**:Spring AI 自动
1. 根据 POJO 生成 JSON Schema
2. 在 prompt 里告诉 LLM "按这个 schema 输出"
3. LLM 输出 JSON
4. Jackson 反序列化 → POJO

**类比**:
```
.content()          ≈  Map<String, Object>     (自由,但要解析)
.entity(Person.class) ≈  Person                  (强类型,开箱用)
```

## 关键概念(4 个)

### 概念 1:`.entity(Class<T>)`

最简单形式,单 POJO:

```java
public record Person(String name, int age, String occupation) {}

Person p = client.prompt()
    .user("提取: 'Bob,30 岁,Java 工程师'")
    .call()
    .entity(Person.class);
```

**底层 3 步**:
1. Spring AI 把 Person 类的字段生成 JSON Schema
2. 在 prompt 注入 schema:`{ "name": "...", "age": 0, "occupation": "..." }`
3. LLM 按 schema 输出,Spring AI 用 Jackson 解析

### 概念 2:`.entity(ParameterizedTypeReference<T>)`

处理泛型(必须用匿名类,因为 Java 泛型擦除):

```java
// List<Movie> — 泛型,没法直接 .entity(List<Movie>.class)
List<Movie> movies = client.prompt()
    .user("推荐 3 部电影")
    .call()
    .entity(new ParameterizedTypeReference<List<Movie>>() {});   // ← 匿名类
```

**为什么不能 `List<Movie>.class`?**
- Java 泛型擦除,运行期 `List<Movie>` 就是 `List`
- 匿名子类保留泛型信息,Spring AI 用反射读
- 这是 Spring 生态统一约定(RestTemplate / WebClient 同样用法)

### 概念 3:record(Java 17)

LLM 输出的 POJO 强烈推荐用 **record**:

```java
public record Person(String name, int age, String occupation) {}
//                ^constructor params + getters + equals + hashCode + toString all auto-generated
```

**为什么 record 适合 LLM 输出**:
- 不可变(LLM 输出应该是只读的)
- 自动 getter(Lombok 替代)
- 短(2 行定义一个 POJO)
- Java 17 标配

### 概念 4:JSON Schema 生成

Spring AI 用 **Jackson** 反射 POJO 生成 JSON Schema:

```java
public record Movie(String title, int year, String director, double rating) {}
```

自动生成 schema(简化):

```json
{
  "type": "object",
  "properties": {
    "title": {"type": "string"},
    "year": {"type": "integer"},
    "director": {"type": "string"},
    "rating": {"type": "number"}
  },
  "required": ["title", "year", "director", "rating"]
}
```

LLM 看到这个 schema,就知道要输出 `{"title": "Interstellar", "year": 2014, ...}`。

**Jackson 注解**影响 schema:

```java
public record Movie(
    @JsonPropertyDescription("Movie title in original language") String title,
    @JsonPropertyDescription("Release year as integer") int year,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate releaseDate
) {}
```

## 4 个实战场景

### 场景 1:单 POJO + record

```java
public record Person(String name, int age, String occupation, String hobby) {}

Person p = client.prompt()
    .user("""
        从这句话提取人物:
        "Bob 是一个 30 岁的 Java 工程师,业余喜欢爬山和摄影。"
        """)
    .call()
    .entity(Person.class);

// → Person[name=Bob, age=30, occupation=Java 工程师, hobby=爬山和摄影]
```

### 场景 2:List<POJO> + ParameterizedTypeReference

```java
public record Movie(String title, int year, String director, double rating) {}

List<Movie> movies = client.prompt()
    .user("""
        推荐 3 部经典科幻电影,按年份升序:
        - 标题(英文)
        - 年份
        - 导演
        - 评分(0-10)
        """)
    .call()
    .entity(new ParameterizedTypeReference<List<Movie>>() {});

// → 3 部电影的 List
```

### 场景 3:enum(情感分类)

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

// → Review[text=这个手机..., sentiment=NEGATIVE]
```

**注意**:LLM 必须输出 enum 字符串("NEGATIVE" / "POSITIVE" / "NEUTRAL")。如果 LLM 输出其他值,Jackson 解析失败。

### 场景 4:嵌套对象

```java
public record Address(String city, String country) {}
public record Customer(String name, Address address, List<String> tags) {}

Customer c = client.prompt()
    .user("提取: '客户 Alice,住北京,中国,标签:VIP / 重要'")
    .call()
    .entity(Customer.class);

// → Customer[name=Alice, address=Address[city=北京, country=中国], tags=[VIP, 重要]]
```

**LLM 嵌套补全技巧**:
- schema 嵌套越深,LLM 出错率越高
- 嵌套 > 3 层建议拆多次调用

## Jackson 注解实战

### `@JsonPropertyDescription` — 字段描述

```java
public record Movie(
    @JsonPropertyDescription("Movie title in original language, e.g. 'Interstellar'") String title,
    @JsonPropertyDescription("Release year as 4-digit integer, e.g. 2014") int year
) {}
```

description 进 schema,LLM 看得更清楚 → 准确率提升。

### `@JsonFormat` — 日期格式

```java
public record Event(
    String name,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date
) {}

Event e = client.prompt().user("'2026 年 8 月 12 日 Spring 大会'").call().entity(Event.class);
// LLM 输出: {"name": "Spring 大会", "date": "2026-08-12"}
```

### `@JsonProperty` — 别名

```java
public record Review(
    @JsonProperty("comment_text") String text    // LLM 输出 "comment_text",Java 字段 "text"
) {}
```

## 不合规输出兜底

LLM 偶尔输出不合规 JSON(虽然 Spring AI 用 JSON mode 强制,99% 合规),兜底策略:

```java
try {
    Person p = client.prompt().user("...").call().entity(Person.class);
} catch (JsonProcessingException e) {
    // 1) 重试
    Person p = client.prompt().user("...").call().entity(Person.class);

    // 2) 降级:拿 String 手动解析
    String raw = client.prompt().user("...").call().content();
    return objectMapper.readValue(raw, Person.class);
}
```

**降级策略**:
1. 加 `temperature=0.0`(确定性,减少随机)
2. 简化 schema(少字段 / 浅嵌套)
3. 加 prompt 强调("严格按 JSON 输出,不要带任何额外文字")

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test
    void contextLoads() {
        // 0 网络(entity 不会主动调 LLM)
    }
}
```

**要真测**:
- mock ChatClient,验证 .entity() 被调用
- 或在测试里手动喂 prompt + 验证 schema

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| LLM 输出不符合 schema | 抛 `JsonProcessingException` | temperature=0 + 加 `format` 指令 |
| 嵌套对象 LLM 不会填 | 字段为 null | 简化 schema / 拆多次调用 |
| 日期类型 | 反序列化失败 | 用 `LocalDate` 配 `@JsonFormat(pattern = "yyyy-MM-dd")` |
| `enum` 值不在定义里 | 抛异常 | 改用 `String` 接受 + 手动转换 |
| `List<Movie>.class` 编译错 | 泛型擦除 | 改 `new ParameterizedTypeReference<List<Movie>>() {}` |
| LLM 输出多余文字 | 解析失败 | system prompt 加 "只输出 JSON,不要其他" |
| 字段名 snake_case vs camelCase | 解析失败 | `@JsonProperty("user_name")` 别名 |
| 数字字段 LLM 输出 "3.5" | int 解析失败 | 用 double / 加 @JsonFormat |

## 实战部署清单

- [ ] 用 record 定义 POJO(不可变 + 短)
- [ ] 关键字段加 `@JsonPropertyDescription`(提升 LLM 准确率)
- [ ] 日期用 `LocalDate` + `@JsonFormat`
- [ ] 嵌套 > 3 层拆多次调用
- [ ] temperature 设 0.0(确定性)
- [ ] system prompt 加 "严格按 JSON 输出"
- [ ] 加 `try-catch` 兜底不合规输出
- [ ] 监控:entity 解析失败率(Actuator)
- [ ] `mvn test` 0 网络 PASS

## 完整代码

[01-basics/05-structured-output/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/05-structured-output)

## 下一步

- [第 6 章 · Streaming →](06-streaming.md)— WebFlux + SSE 实时推流
- [第 13 章 · Agent Basics →](13-agent-basics.md)— @Tool + LLM 完整 Agent 实战
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
