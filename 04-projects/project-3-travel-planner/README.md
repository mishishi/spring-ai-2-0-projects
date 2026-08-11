# Project 3: AI 旅行规划师

> Phase 4 实战项目 3/5 · Multi-Agent 编排(4 个 sub-agent + 1 orchestrator)

## 解决什么问题

用户说"想去海滨,5 天,预算 8000" — 一次性输出目的地推荐、行程、预算、订票建议。
本项目用 4 个 **sub-agent**(Destination/Itinerary/Budget/Booking)+ 1 个 **Orchestrator** 协作,
每个 agent 专职一块,最后 Orchestrator 整合成完整旅行计划。

## 架构

```
  POST /travel/plan
  { theme, days, budgetCNY }
        │
        ▼
  ┌──────────────────────────────────────┐
  │ TravelOrchestrator                   │
  │   1. DestinationAgent → 3 候选        │
  │   2. ItineraryAgent  → N 天行程       │
  │   3. BudgetAgent     → 费用表         │
  │   4. BookingAgent    → 订票 checklist │
  │   5. Orchestrator    → 整合 markdown  │
  └──────────────────────────────────────┘
        │
        ▼
  # 🌍 海滨 · 5 天旅行计划
  ## 1. 目的地推荐
  - 三亚 ...
  - 厦门 ...
  ## 2. 行程安排
  Day 1: 抵达 + 城市漫步
  Day 2-4: 核心景点
  Day 5: 返程
  ## 3. 预算估算
  | 类别 | 单日 | 小计 |
  | 住宿 | 640 | 3200 |
  ...
  ## 4. 订票策略
  - [ ] 提前 30 天订机票 ...
```

## 关键技术

- **Multi-Agent 编排模式** (Anthropic Orchestrator-Workers)
- **4 个 ChatClient Bean**:每个有独立 system prompt 角色
- **Orchestrator**:调度 sub-agent,整合结果
- **0 网络 mock 路径**:sub-agent bean 为 null 时用规则引擎生成(主题→目的地映射)

## 跑起来

```bash
# 0 网络测试
mvn -pl 04-projects/project-3-travel-planner test

# 真实跑(4 个 ChatClient 真实 LLM 协作)
export OPENAI_API_KEY=sk-xxxxx
cd 04-projects/project-3-travel-planner
mvn spring-boot:run

# 调用
curl -X POST http://localhost:8080/travel/plan \
  -H 'Content-Type: application/json' \
  -d '{"theme": "海滨", "days": 5, "budgetCNY": 8000}'
```

## 核心代码

- `TravelAgentsConfig` — 4 个 sub-agent + 1 orchestrator 的 ChatClient Bean
- `TravelPlannerService` — 编排流程
- `TravelPlannerController` — POST /travel/plan
- `TravelRequest` / `TravelPlan` — 入参 / 出参 record

## 学到啥

- Spring AI 2.0 Multi-Agent 模式(章节 16)
- ChatClient Bean 隔离(每个 agent 独立 system prompt)
- Orchestrator-Workers 协作模式
- 0 网络 mock 与真实 LLM 的切换
- record 类型在 Spring Boot 里的最佳实践

## 扩展方向

- 真实 LLM 时,sub-agent 互相调(用 FunctionToolCallback)
- 加 PDF 输出(iText / OpenPDF)
- 加日历集成(Google Calendar API 写入行程)
- 多语言(英文 / 日文目的地推荐)
