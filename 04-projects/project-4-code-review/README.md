# Project 4: AI 代码审查器

> Phase 4 实战项目 4/5 · @Tool 静态分析 + ChatClient 语义审查

## 解决什么问题

PR review 费时,基础反模式(空 catch / 硬编码密码 / 嵌套过深)完全可以机器检查,
AI Agent 用 **@Tool 调用静态分析工具** 找出客观问题,
再用 **ChatClient 做语义审查** 给出"亮点 / 建议"等主观意见。

## 架构

```
  POST /review/code
  { code, language }
        │
        ▼
  ┌──────────────────────────────────┐
  │ CodeReviewService                │
  │   1. @Tool countLines(code)      │
  │   2. @Tool detectAntiPatterns()  │──▶ 静态扫描
  │   3. @Tool estimateComplexity()  │
  │   4. ChatClient.semanticReview() │──▶ AI 语义审查
  │   5. computeScore()              │──▶ 0-100 评分
  └──────────────────────────────────┘
        │
        ▼
  {
    "language": "java",
    "lineCount": { "total": 8, "code": 5, "blank": 2, "comment": 1 },
    "antiPatterns": [
      { "name": "空 catch 块", "severity": "high", ... }
    ],
    "complexity": { "complexity": 6, "level": "moderate" },
    "semanticReview": "📊 综合评分: 70/100\n\n✅ 必须修改: ...",
    "score": 70
  }
```

## 核心 @Tool

| 工具 | 作用 | 输出 |
|------|------|------|
| `countLines` | 行数统计 | total / blank / comment / code |
| `detectAntiPatterns` | 反模式检测 | 列表(name / severity / suggestion) |
| `estimateComplexity` | 圈复杂度 | complexity(数字) + level(简单/中等/复杂/不可测) |

检测的反模式:
- 空 catch 块
- System.out.println / console.log
- 硬编码密码
- SQL 字符串拼接
- TODO / FIXME
- 超长行(>200 字符)
- 嵌套过深(>4 层)

## 跑起来

```bash
# 0 网络测试
mvn -pl 04-projects/project-4-code-review test

# 真实跑
export OPENAI_API_KEY=sk-xxxxx
cd 04-projects/project-4-code-review
mvn spring-boot:run

# 调用
curl -X POST http://localhost:8080/review/code \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "public void m() { try { doIt(); } catch (Exception e) { } }",
    "language": "java"
  }'
```

## 核心代码

- `CodeAnalysisTools` — 3 个 @Tool 方法 + 3 个 record DTO
- `CodeReviewService` — 工具调用编排 + 评分算法
- `CodeReviewController` — POST /review/code

## 学到啥

- Spring AI 2.0 `@Tool` 注解(章节 14)
- Tool + ChatClient 协作模式
- 静态分析规则(可以扩展接 SonarQube / Checkstyle)
- 综合评分算法(每个 severity 扣分)
- 0 网络 mock 与真实 LLM 的双路径

## 扩展方向

- 加 TreeSitter / JavaParser 做 AST 级分析
- 集成 Checkstyle / SpotBugs / PMD
- Git diff 集成(只 review 改动行)
- PR 平台集成(GitHub / GitLab webhook)
- 多语言(JS / Python / Go 各加规则)
