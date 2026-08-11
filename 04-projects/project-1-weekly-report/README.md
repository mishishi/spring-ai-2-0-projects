# Project 1: AI 周报生成器

> Phase 4 实战项目 1/5 · 20 分钟跑起来

## 解决什么问题

工程师每周写周报是重复劳动 — 把本周完成的工作条目整理成结构化文档。
本项目用 Spring AI 2.0 ChatClient + PromptTemplate,**输入** 工作条目列表,
**输出** 结构化中文 markdown 周报。

## 架构

```
┌──────────────────┐
│ POST /report/    │
│ generate         │
└────────┬─────────┘
         │ { completed, planned, blockers }
         ▼
┌──────────────────────────┐
│ WeeklyReportService       │
│   - ChatClient            │
│   - defaultSystem(中文周报) │
└────────┬──────────────────┘
         │ 润色 + 结构化
         ▼
   # 周报 · 2026-08-12
   ## 本周完成
   - ...
   ## 下周计划
   - ...
   ## 风险与阻塞
   - ...
   ## 数据指标
   - 本周完成: 3 项
   ...
```

## 跑起来

```bash
# 0 网络(用 fake key 测模板逻辑)
mvn -pl 04-projects/project-1-weekly-report test

# 真实跑
export OPENAI_API_KEY=sk-xxxxx
cd 04-projects/project-1-weekly-report
mvn spring-boot:run

# 调用
curl -X POST http://localhost:8080/report/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "completed": ["完成 Phase 1", "修了 N 个 bug"],
    "planned": ["开始 Phase 2"],
    "blockers": ["等设计稿"]
  }'
```

## 核心代码

- `WeeklyReportService` — ChatClient + 0 网络 mock
- `WeeklyReportController` — POST /report/generate

## 学到啥

- Spring AI 2.0 ChatClient 基础使用
- PromptTemplate 结构化输入
- 0 网络测试 fallback(测试模板而非真实 LLM)
- Spring Boot 完整项目骨架

## 扩展方向

- 加 Stream 流式输出(降低首字延迟)
- 集成 git log 自动收集 commit
- 集成日历 API 自动拉"本周会议"
- 多周趋势图(完成项数 vs 计划项数)
