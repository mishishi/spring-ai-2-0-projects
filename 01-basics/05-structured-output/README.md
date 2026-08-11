# 第 5 章 · Structured Output

> 🎯 目标:把 LLM 的字符串输出转成强类型 Java 对象,告别手写 JSON 解析

## 你将学到

- ✅ `.entity(Class<T>)` — 拿单个 POJO
- ✅ `.entity(ParameterizedTypeReference<T>)` — 拿 List<T> / 复杂泛型
- ✅ record class vs 普通 class 的选择
- ✅ LLM 输出不稳定时怎么处理(JSON 解析失败)

## 快速开始

```bash
cd 01-basics/05-structured-output
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

2 个 demo:

```
══════ Demo 1: entity(Person.class) ══════
🤖 Person[name=Bob, age=30, occupation=Java 工程师, hobby=爬山和摄影]
   name = Bob
   age = 30
   occupation = Java 工程师
   hobby = 爬山和摄影

══════ Demo 2: ParameterizedTypeReference<List<Movie>> ══════
🤖 Blade Runner (1982) - Ridley Scott - 评分 8.1
🤖 The Matrix (1999) - Wachowski - 评分 8.7
🤖 Interstellar (2014) - Christopher Nolan - 评分 9.0
```

## 关键代码

### 1. POJO(record 写法)

```java
public record Person(String name, int age, String occupation, String hobby) {}
```

### 2. 拿单 POJO

```java
Person person = client.prompt()
    .user("从这句话提取人物信息: 'Bob,30 岁,Java 工程师,爱爬山'")
    .call()
    .entity(Person.class);

System.out.println(person.name());  // "Bob"
```

### 3. 拿 List<POJO>

```java
List<Movie> movies = client.prompt()
    .user("推荐 3 部科幻电影...")
    .call()
    .entity(new ParameterizedTypeReference<List<Movie>>() {});
```

**为什么需要 ParameterizedTypeReference?**
因为 Java 类型擦除 — `List.class` 不知道元素类型,必须用 `TypeReference` 保留泛型信息。

## 3 种 POJO 风格

### 1. record(推荐,Java 16+)

```java
public record Person(String name, int age, String occupation) {}
```

✅ 简洁 / immutable / 自动 getter

### 2. 普通 class + getter/setter

```java
public class Person {
    private String name;
    private int age;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ...
}
```

✅ Jackson 兼容最好 / 可变

### 3. Lombok @Data

```java
@Data
public class Person {
    private String name;
    private int age;
}
```

✅ 简洁 / 需要 Lombok 依赖

## 字段约束技巧

LLM 不会精确匹配字段名,要给它**清晰指令**或者**Jackson 注解**提示:

### 方案 1:在 prompt 里说清楚

```java
.user("""
    提取人物信息,严格按以下字段:
    - name (String): 人物名字
    - age (int): 年龄(数字)
    - occupation (String): 职业
    """)
```

### 方案 2:@JsonProperty(Java 字段 ↔ JSON key)

```java
public record Person(
    @JsonProperty("full_name") String name,
    @JsonProperty("years_old") int age,
    String occupation) {}
```

### 方案 3:@JsonAlias(兼容多种写法)

```java
public record Person(
    @JsonAlias({"name", "姓名", "Name"}) String name,
    @JsonAlias({"age", "年龄", "Age"}) int age) {}
```

## 嵌套对象

```java
public record Company(String name, Address address) {}
public record Address(String city, String country) {}

// LLM 也能解析嵌套
Company c = client.prompt()
    .user("介绍一家公司: '阿里巴巴总部在杭州,中国'")
    .call()
    .entity(Company.class);
// Company[name=阿里巴巴, address=Address[city=杭州, country=中国]]
```

## 实战模式

| 场景 | 输出类型 |
|---|---|
| 信息提取 | 单 POJO / List<POJO> |
| 表单填写 | 嵌套 POJO |
| 分类 | `enum`(LLM 输出 enum 值) |
| 评分 | `int` / `double` |
| 校验 | POJO + Bean Validation(`@NotNull` 等) |

## 测试

```bash
mvn test
```

0 网络。

## 目录结构

```
05-structured-output/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/structuredoutput/
    │   │   ├── Application.java
    │   │   ├── Person.java
    │   │   └── Movie.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/cc/misshi/springai/structuredoutput/
            └── ApplicationTests.java
```

## 下一章

[第 6 章 · Streaming →](../06-streaming/README.md)

用 Spring Boot WebFlux + SSE 把 LLM 输出流式推到浏览器
