# P4 · AI 代码审查器


## 一句话总结

**@Tool 静态扫描 + ChatClient 语义审查** — PR review 自动化:客观反模式机器查,主观意见 LLM 评。

## 你将学到

- ✅ `@Tool` 注解实战(章节 13/14 进阶)
- ✅ 静态代码分析 7 大反模式检测
- ✅ 圈复杂度估算(McCabe)
- ✅ 综合评分算法(severity 扣分)
- ✅ Spring Boot + @Tool 完整项目骨架

## 快速开始

```bash
cd 04-projects/project-4-code-review
mvn test                          # 0 网络 10 tests, ~6s
mvn spring-boot:run

curl -X POST http://localhost:8080/review/code \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "public void m() { try { doIt(); } catch (Exception e) { } }",
    "language": "java"
  }'
```

## 架构

```
  POST /review/code
  { code, language }
        │
        ▼
  ┌────────────────────────────────────┐
  │ CodeReviewService                   │
  │   1. @Tool countLines(code)         │  ← 静态分析
  │   2. @Tool detectAntiPatterns()     │
  │   3. @Tool estimateComplexity()     │
  │   4. ChatClient.semanticReview()    │  ← AI 语义
  │   5. computeScore() → 0-100        │
  └────────────┬───────────────────────┘
               │
               ▼
  {
    "language": "java",
    "lineCount": {"total": 1, "code": 1, "blank": 0, "comment": 0},
    "antiPatterns": [
      {"name": "空 catch 块", "severity": "high", "suggestion": "..."}
    ],
    "complexity": {"complexity": 2, "level": "simple"},
    "semanticReview": "📊 综合评分: 85/100\n\n⚠️ 必须修改: 1 项...",
    "score": 85
  }
```

## 关键代码

### 1. 3 个 @Tool 方法

```java
@Component
public class CodeAnalysisTools {

    @Tool(description = "统计代码行数,返回 total/blank/comment/code")
    public LineCountResult countLines(String code, String language) {
        // 按 \n 切行,识别空行 / 注释行
        ...
    }

    @Tool(description = "检测常见反模式(空 catch / System.out / 硬编码密码 / SQL 拼接 / TODO / 超长行 / 嵌套过深)")
    public List<AntiPatternHit> detectAntiPatterns(String code, String language) {
        // 7 个 if / regex
        ...
    }

    @Tool(description = "估算圈复杂度(if/while/for/case/catch/and/or 各 +1)")
    public ComplexityResult estimateComplexity(String code) {
        // 关键字计数 + 1 baseline
        ...
    }
}
```

**LLM 调工具流程**:
1. LLM 看到代码 + 反模式检测需求
2. 自动调 `detectAntiPatterns(code, "java")`
3. 拿到 `List<AntiPatternHit>`(JSON)
4. 整理成"必须修改 / 建议改进 / 亮点"3 段

### 2. 7 大反模式

```java
public List<AntiPatternHit> detectAntiPatterns(String code, String language) {
    List<AntiPatternHit> hits = new ArrayList<>();
    if (code == null || code.isEmpty()) return hits;

    // 1. 空 catch
    Pattern emptyCatch = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}");
    if (emptyCatch.matcher(code).find()) {
        hits.add(new AntiPatternHit("空 catch 块", "high", "至少 log.error(...) 或重新抛出"));
    }

    // 2. System.out.println
    if (code.contains("System.out.println")) {
        hits.add(new AntiPatternHit("System.out.println", "low", "生产应改用 logger"));
    }

    // 3. 硬编码密码
    Pattern passwordPattern = Pattern.compile("(?i)(password|passwd|pwd)\\s*=\\s*[\"']([^\"']+)[\"']");
    if (passwordPattern.matcher(code).find()) {
        hits.add(new AntiPatternHit("硬编码密码", "high", "应使用环境变量 / 配置中心"));
    }

    // 4. SQL 拼接(简化检测)
    if (code.contains("Statement ") && code.contains("executeQuery")) {
        hits.add(new AntiPatternHit("可能存在 SQL 拼接", "high", "应使用 PreparedStatement"));
    }

    // 5. TODO / FIXME
    if (code.contains("TODO") || code.contains("FIXME")) {
        hits.add(new AntiPatternHit("TODO / FIXME", "low", "提交前应清理"));
    }

    // 6. 超长行
    for (String line : code.split("\n")) {
        if (line.length() > 200) {
            hits.add(new AntiPatternHit("超长行", "low", "建议拆行"));
            break;
        }
    }

    // 7. 嵌套过深
    if (computeMaxNestingDepth(code) > 4) {
        hits.add(new AntiPatternHit("嵌套深度 > 4", "medium", "建议抽取方法"));
    }

    return hits;
}
```

### 3. 圈复杂度(McCabe)

```java
public ComplexityResult estimateComplexity(String code) {
    int complexity = 1;  // baseline
    String[] keywords = {"if", "else if", "while", "for", "case", "catch", "&&", "||", "?"};
    for (String kw : keywords) {
        int idx = 0;
        while ((idx = code.indexOf(kw, idx)) != -1) {
            complexity++;
            idx += kw.length();
        }
    }
    String level;
    if (complexity <= 5) level = "simple";
    else if (complexity <= 10) level = "moderate";
    else if (complexity <= 20) level = "complex";
    else level = "untestable";
    return new ComplexityResult(complexity, level);
}
```

McCabe 经典公式:**复杂度 = 1 + 分支数**。

### 4. 综合评分

```java
private int computeScore(List<AntiPatternHit> hits, ComplexityResult cx, LineCountResult lc) {
    int score = 100;
    for (var h : hits) {
        score -= switch (h.severity()) {
            case "high" -> 15;
            case "medium" -> 8;
            case "low" -> 3;
            default -> 1;
        };
    }
    if ("complex".equals(cx.level())) score -= 10;
    if ("untestable".equals(cx.level())) score -= 20;
    return Math.max(0, score);
}
```

**评分规则**:
- 每个 high -15, medium -8, low -3
- 圈复杂度 complex -10, untestable -20
- 最低 0,最高 100

### 5. 0 网络 Mock 语义审查

```java
private String mockSemanticReview(List<AntiPatternHit> hits,
                                   ComplexityResult cx,
                                   LineCountResult lc) {
    long high = hits.stream().filter(h -> "high".equals(h.severity())).count();
    long medium = hits.stream().filter(h -> "medium".equals(h.severity())).count();
    long low = hits.stream().filter(h -> "low".equals(h.severity())).count();

    StringBuilder sb = new StringBuilder("【AI 语义审查 · Mock】\n\n");
    sb.append("📊 综合评分: ").append(score).append("/100\n\n");
    if (high == 0 && medium == 0) sb.append("✅ 必须修改: 无\n");
    else { /* list high hits */ }
    if (low + medium > 0) { /* list low/medium */ }
    sb.append("🎯 亮点:\n");
    if (lc.comment() > lc.code() * 0.2) sb.append("注释比例 ").append(...).append(", 文档良好\n");
    if ("simple".equals(cx.level())) sb.append("圈复杂度低,易测\n");
    return sb.toString();
}
```

真实 LLM 时,`chatClient.prompt().user(拼好的 prompt).call().content()`,LLM 整理成"自然"review。

## 3 个 Demo

### Demo 1: 空 catch 检测

```bash
curl -X POST http://localhost:8080/review/code -d '{
  "code": "public void m() { try { doIt(); } catch (Exception e) { } }",
  "language": "java"
}'
```

返回 antiPatterns 包含 "空 catch 块",severity=high,score=85。

### Demo 2: 复杂代码

```bash
curl -X POST http://localhost:8080/review/code -d '{
  "code": "if (a) { while (b) { for (int i = 0; i < 10; i++) { if (c && d) { doIt(); } } } }",
  "language": "java"
}'
```

返回 complexity=6, level="moderate"。

### Demo 3: 综合代码

```bash
curl -X POST http://localhost:8080/review/code -d '{
  "code": "public void m() { String password = \"secret\"; System.out.println(\"hi\"); try {} catch (Exception e) {} }",
  "language": "java"
}'
```

返回多条反模式 + 低分(60 左右)。

## 踩坑(3 大常见)

### 坑 1: Java 17 不支持 patterns in switch

```java
// ❌ Java 17 编译失败
String level = switch (complexity) {
    case int c when c <= 5 -> "simple";
    ...
};

// ✅ 传统 if-else
if (complexity <= 5) level = "simple";
else if (complexity <= 10) level = "moderate";
```

### 坑 2: 复杂度算错(漏算边界)

```java
// ❌ 只算 "if" 不算 "else if"
String[] keywords = {"if", "while", "for"};

// ✅ 全部关键词
String[] keywords = {"if", "else if", "while", "for", "case", "catch", "&&", "||", "?"};
```

### 坑 3: 评分可能负数

```java
// ❌ high 问题 10 个 → score = 100 - 150 = -50
int score = 100;
for (hit : hits) score -= 15;

// ✅ clamp
return Math.max(0, score);
```

## 0 网络测试

10 tests:
- countLines 各种 case
- detectAntiPatterns 7 个反模式各 1 test
- estimateComplexity 高/低
- ReviewService 完整流程

## 实战清单

- [x] 7 大反模式检测
- [x] 圈复杂度估算
- [x] 综合评分
- [x] Mock 语义审查
- [x] @Tool 完整项目骨架
- [ ] **生产补 1**:TreeSitter / JavaParser 做 AST 级分析
- [ ] **生产补 2**:Checkstyle / SpotBugs 集成
- [ ] **生产补 3**:GitHub / GitLab PR webhook 集成

## 完整代码

[04-projects/project-4-code-review/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/04-projects/project-4-code-review)

## 学完下一步

[P5 AI 综合知识中心 →](project-5-knowledge-hub.md) — 整合 P1-P4 能力,RAG + Tool + Memory + Multi-Agent 一体化。
