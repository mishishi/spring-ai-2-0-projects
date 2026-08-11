# P1 · AI 周报生成器


## 一句话总结

**工程师周报自动化** — 输入工作条目列表,ChatClient 整理成结构化中文 markdown 周报;0 网络走 mock 模板,真 LLM 一行切换。

## 你将学到

- ✅ Spring Boot 完整项目骨架(Controller / Service / DTO)
- ✅ ChatClient 真实用法(`prompt().user().call().content()`)
- ✅ 0 网络 mock 模式(测试模板逻辑而非真 LLM)
- ✅ Null-check fallback Pattern(`builder == null → mock`)

## 快速开始

```bash
cd 04-projects/project-1-weekly-report
mvn test                          # 0 网络 3 tests, ~5s
mvn spring-boot:run               # 真实跑 (需要 OPENAI_API_KEY)

curl -X POST http://localhost:8080/report/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "completed": ["完成 Phase 1 基础章节", "修复 3 个 Spring AI 2.0 bug"],
    "planned": ["开始 Phase 2 RAG", "写项目实战 P1 周报"],
    "blockers": ["等设计稿"]
  }'
```

## 架构

```
  POST /report/generate
  { completed, planned, blockers }
        │
        ▼
  ┌────────────────────────────────────┐
  │ WeeklyReportService                 │
  │   1. 拼 userPrompt                  │
  │   2. chatClient == null?            │
  │     ├─ Yes → buildMockReport()     │  0 网络
  │     └─ No  → chatClient.prompt()  │  真实 LLM
  └────────────┬───────────────────────┘
               │
               ▼
  # 周报 · 2026-08-12
  ## 本周完成
  - 完成 Phase 1 ...
  - 修复 3 个 bug
  ## 下周计划
  - 开始 Phase 2 ...
  ## 风险与阻塞
  - 等设计稿
  ## 数据指标
  - 本周完成: 2 项
  - 下周计划: 2 项
  - 当前阻塞: 1 项
```

## 关键代码

### 1. Service 注入 ChatClient.Builder

```java
@Service
public class WeeklyReportService {
    private final ChatClient chatClient;

    public WeeklyReportService(ChatClient.Builder builder) {
        this.chatClient = (builder == null) ? null : builder
                .defaultSystem("""
                        你是 helpful 助理,负责把工程师工作条目整理成结构化中文周报。
                        风格:简洁、客观、突出价值。
                        输出:Markdown 格式,4 个 H2 段落(本周完成 / 下周计划 / 风险与阻塞 / 数据指标)。
                        """)
                .build();
    }

    public String generate(List<String> completed, List<String> planned, List<String> blockers) {
        String userPrompt = buildUserPrompt(completed, planned, blockers);
        if (chatClient == null) {
            return buildMockReport(completed, planned, blockers);
        }
        return chatClient.prompt().user(userPrompt).call().content();
    }
}
```

**核心 Pattern**:
- `builder == null` 时,chatClient 为 null,走 mock
- 测试场景: `new WeeklyReportService(null)` → 0 网络
- Spring 容器: 注入 `ChatClient.Builder` → 真实 LLM

### 2. Mock 模板

```java
private String buildMockReport(List<String> completed, List<String> planned, List<String> blockers) {
    return """
            # 周报 · %s

            ## 本周完成
            %s

            ## 下周计划
            %s

            ## 风险与阻塞
            %s

            ## 数据指标
            - 本周完成: %d 项
            - 下周计划: %d 项
            - 当前阻塞: %d 项
            """.formatted(
            LocalDate.now(),
            formatList(completed),
            formatList(planned),
            formatList(blockers),
            completed.size(), planned.size(), blockers.size()
    );
}
```

模板化 + 数据指标统计,无需 LLM 也能产出可用周报。

### 3. Controller

```java
@RestController
@RequestMapping("/report")
public class WeeklyReportController {
    private final WeeklyReportService service;

    public WeeklyReportController(WeeklyReportService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public String generate(@RequestBody ReportRequest req) {
        return service.generate(req.completed(), req.planned(), req.blockers());
    }

    public record ReportRequest(List<String> completed, List<String> planned, List<String> blockers) {}
}
```

## 3 个 Demo

### Demo 1: 标准周报

```bash
curl -X POST http://localhost:8080/report/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "completed": ["完成 A", "修复 B", "code review 5 个 PR"],
    "planned": ["开始 C", "集成测试"],
    "blockers": []
  }'
```

返回完整 markdown 周报。

### Demo 2: 空输入

```bash
curl -X POST http://localhost:8080/report/generate \
  -H 'Content-Type: application/json' \
  -d '{"completed": [], "planned": [], "blockers": []}'
```

返回周报但每段都是空 list,数据指标全 0。

### Demo 3: 真实 LLM 跑

```bash
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
# 同样 curl 调,这次 ChatClient 真去问 OpenAI,产出更"自然"的周报
```

## 0 网络 + 真实 LLM 双路径 Pattern

```java
// 测试代码 — 0 网络
WeeklyReportService service = new WeeklyReportService(null);
String mockReport = service.generate(...);
assertThat(mockReport).contains("本周完成");

// Spring 容器 — 真实 LLM
@SpringBootTest
class IntegrationTest {
    @Autowired WeeklyReportService service;  // chatClient 注入成功
}
```

**好处**:
- 单元测试秒过(0 网络)
- 集成测试不写(直接 `mvn spring-boot:run` 测)
- CI pipeline 不会因为 LLM 限流挂掉

## 踩坑(3 大常见)

### 坑 1: `Builder` 不是 `@Autowired` 必需

```java
// ❌ 启动报错
public WeeklyReportService(ChatClient.Builder builder) {
    this.chatClient = builder.defaultSystem("...").build();
}

// ✅ null-safe
public WeeklyReportService(ChatClient.Builder builder) {
    this.chatClient = (builder == null) ? null : builder.defaultSystem("...").build();
}
```

### 坑 2: `List<String>` 没判空

```java
// ❌ NPE
String list = String.join("\n- ", completed);  // completed == null

// ✅ 防御
String list = completed == null ? "(无)" : String.join("\n- ", completed);
```

### 坑 3: `LocalDate.now()` 注入测试不友好

测试断言"周报"包含日期时:
```java
assertThat(report).contains(LocalDate.now().toString());  // ✅ 同一天跑测试 OK
```

## 0 网络测试

3 tests:
- `service 构造不抛异常` (null builder 接受)
- `generate 返回 markdown`
- `数据指标正确` (完成 / 计划 / 阻塞 计数)

## 实战清单

- [x] Spring Boot MVC 完整骨架
- [x] ChatClient 真实用法
- [x] 0 网络 mock 模式
- [x] Null-check fallback
- [ ] **生产补 1**:Streaming 流式输出(降低首字延迟)
- [ ] **生产补 2**:集成 git log 自动收集 commit
- [ ] **生产补 3**:周报趋势图(每周完成项数)

## 完整代码

[04-projects/project-1-weekly-report/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/04-projects/project-1-weekly-report)

## 学完下一步

[P2 企业文档问答 →](project-2-doc-qa.md) — RAG 实战,中文 2-gram 关键词检索 + ChatClient 真 LLM 路径。
