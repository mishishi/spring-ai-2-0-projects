# P3 · AI 旅行规划师


## 一句话总结

**Multi-Agent 编排实战** — 用户给"海滨/雪山/美食/文化/通用"主题 + 天数 + 预算,4 个 sub-agent 协作(Destination / Itinerary / Budget / Booking)+ 1 Orchestrator,产出完整 markdown 旅行计划。

## 你将学到

- ✅ Multi-Agent Orchestrator-Workers 模式(章节 16 实战)
- ✅ 4 个独立 ChatClient Bean,每个专职一块
- ✅ 0 网络 mock 路径(规则引擎生成计划)
- ✅ `record` DTO 最佳实践(Spring Boot)
- ✅ 主题→目的地的语义映射

## 快速开始

```bash
cd 04-projects/project-3-travel-planner
mvn test                          # 0 网络 4 tests, ~5s
mvn spring-boot:run

curl -X POST http://localhost:8080/travel/plan \
  -H 'Content-Type: application/json' \
  -d '{"theme": "海滨", "days": 5, "budgetCNY": 8000}'
```

## 架构

```
  POST /travel/plan
  { theme, days, budgetCNY }
        │
        ▼
  ┌──────────────────────────────────────┐
  │ TravelOrchestrator                    │
  │   1. DestinationAgent → 3 候选目的地  │
  │   2. ItineraryAgent  → N 天行程       │
  │   3. BudgetAgent     → 费用表         │
  │   4. BookingAgent    → 订票 checklist │
  │   5. Orchestrator    → 整合 markdown  │
  └──────────────────────────────────────┘
        │
        ▼
  # 🌍 海滨 · 5 天旅行计划
  ## 1. 目的地推荐
  - 三亚:11-4 月最佳,亚龙湾 / 蜈支洲岛 / 天涯海角
  - 厦门:3-5 月或 9-11 月,鼓浪屿 / 环岛路 / 沙坡尾
  - 青岛:5-10 月,栈桥 / 八大关 / 崂山
  ## 2. 行程安排
  Day 1: 抵达 + 城市漫步
  Day 2-4: 核心景点
  Day 5: 返程
  ## 3. 预算估算
  | 类别 | 单日 | 小计 |
  | 住宿 | 640 | 3200 |
  ...
  ## 4. 订票策略
  - [ ] 提前 30 天订机票
  ...
```

## 关键代码

### 1. 5 个独立 ChatClient Bean

```java
@Configuration
public class TravelAgentsConfig {

    @Bean(name = "destinationAgent")
    public ChatClient destinationAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 DestinationAgent(目的地顾问)。
                职责: 根据用户兴趣/季节/预算,推荐 3 个候选目的地。
                每个目的地给出名称、最佳季节、3 个亮点。
                输出: markdown bullet list。
                """).build();
    }

    @Bean(name = "itineraryAgent")
    public ChatClient itineraryAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 ItineraryAgent(行程规划师)。
                职责: 把目的地拆成 N 天行程,每天上午/下午/晚上各 1 活动。
                输出: 按天分小节。
                """).build();
    }

    @Bean(name = "budgetAgent")
    public ChatClient budgetAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 BudgetAgent(预算分析师)。
                职责: 估算机票/酒店/餐饮/门票/交通费用,给出总预算区间。
                输出: 表格(类别/单日/小计)。
                """).build();
    }

    @Bean(name = "bookingAgent")
    public ChatClient bookingAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 BookingAgent(票务顾问)。
                职责: 推荐订票时机(提前多久/哪个平台/有无优惠)。
                输出: 简洁 checklist。
                """).build();
    }

    @Bean(name = "travelOrchestrator")
    public ChatClient travelOrchestrator(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 TravelOrchestrator(总协调)。
                接到请求后,你会:
                1. 调 destinationAgent 选 3 候选
                2. 调 itineraryAgent 编排 N 天行程
                3. 调 budgetAgent 估算预算
                4. 调 bookingAgent 推荐订票
                5. 整合为完整 markdown 旅行计划
                """).build();
    }
}
```

### 2. 编排 Service(0 网络 mock 路径)

```java
@Service
public class TravelPlannerService {
    private final ChatClient destinationAgent;
    // ... 5 个 ChatClient 注入

    public TravelPlan plan(TravelRequest req) {
        if (isMockMode()) {
            return mockPlan(req);  // 0 网络
        }
        // 真实 LLM 路径
        String destinations = destinationAgent.prompt()...call().content();
        // ... 其他 3 个 agent
        return new TravelPlan(...);
    }

    private boolean isMockMode() {
        return destinationAgent == null;  // 测试场景
    }

    private TravelPlan mockPlan(TravelRequest req) {
        String theme = req.theme() == null ? "通用" : req.theme();
        int days = Math.max(1, req.days());
        int budget = Math.max(1000, req.budgetCNY());

        String destinations = mockDestinations(theme);
        String itinerary = mockItinerary(theme, days);
        String budgetStr = mockBudget(budget, days);
        String booking = mockBooking();
        String finalPlan = formatPlan(theme, days, budget, ...);

        return new TravelPlan(destinations, itinerary, budgetStr, booking, finalPlan);
    }
}
```

**关键**:
- `isMockMode()` 简单 null check
- 真实 LLM 时,`destinationAgent.prompt().user(...).call().content()` 自动接上

### 3. 主题 → 目的地 映射

```java
private String mockDestinations(String theme) {
    return switch (theme) {
        case "海滨" -> """
                - **三亚**: 11-4 月,亚龙湾 / 蜈支洲岛 / 天涯海角
                - **厦门**: 3-5 月,鼓浪屿 / 环岛路 / 沙坡尾
                - **青岛**: 5-10 月,栈桥 / 八大关 / 崂山
                """;
        case "雪山" -> """
                - **丽江**: 11-4 月,玉龙雪山 / 蓝月谷 / 束河古镇
                - **北海道**: 12-3 月,小樽 / 函馆山
                - **瑞士**: 12-3 月,因特拉肯 / 少女峰
                """;
        // ... 美食 / 文化 / 默认
    };
}
```

**真实 LLM 时**,destinationAgent 自行用 LLM 推理;0 网络用 switch 硬编码映射。

### 4. record DTO

```java
public record TravelRequest(String theme, int days, int budgetCNY) {
    public TravelRequest {
        if (days <= 0) days = 3;
        if (budgetCNY <= 0) budgetCNY = 5000;
    }
}

public record TravelPlan(
        String destinations,
        String itinerary,
        String budget,
        String booking,
        String finalPlan
) {}
```

Java 17 record + 紧凑构造器(参数校验),简洁又安全。

## 3 个 Demo

### Demo 1: 海滨 5 天 8000 元

```bash
curl -X POST http://localhost:8080/travel/plan \
  -H 'Content-Type: application/json' \
  -d '{"theme": "海滨", "days": 5, "budgetCNY": 8000}'
```

返回完整 markdown:三亚/厦门/青岛推荐 + 5 天行程 + 8000 元预算分配 + 订票 checklist。

### Demo 2: 雪山 4 天 12000

```bash
curl -X POST http://localhost:8080/travel/plan \
  -H 'Content-Type: application/json' \
  -d '{"theme": "雪山", "days": 4, "budgetCNY": 12000}'
```

返回丽江/北海道/瑞士推荐。

### Demo 3: 默认参数

```bash
curl -X POST http://localhost:8080/travel/plan \
  -H 'Content-Type: application/json' \
  -d '{}'
```

`TravelRequest` 紧凑构造器自动设置 days=3, budgetCNY=5000, theme="通用"(在 service 里 default)。

## 踩坑(3 大常见)

### 坑 1: Bean 名字跟 `@Qualifier` 不一致

```java
// ❌
@Bean(name = "destination") public ChatClient destinationAgent(...)
public TravelPlannerService(@Qualifier("destination") ChatClient dest) { ... }

// ✅ 一致
@Bean(name = "destinationAgent") ... @Qualifier("destinationAgent")
```

### 坑 2: Sub-agent 串味

每个 sub-agent 的 system prompt **必须严格隔离**:
- DestinationAgent 只输出目的地
- BudgetAgent 只输出费用
- 不要让 budget 顺手推荐目的地(角色混乱)

### 坑 3: 真实 LLM 没传 prompt

```java
// ❌ 只调 LLM,不给上下文
destinationAgent.prompt().call().content();  // LLM 不知道推荐什么

// ✅ 传用户输入
destinationAgent.prompt()
    .user("推荐 " + theme + " 主题 " + days + " 天旅行目的地")
    .call()
    .content();
```

## 0 网络测试

4 tests:
- 完整 5 天海滨计划生成
- 不同主题(雪山/美食)对应不同目的地
- 默认参数(0/0/0 → 默认值)
- TravelRequest 紧凑构造器校验

## 实战清单

- [x] 5 个 ChatClient Bean 隔离
- [x] Orchestrator-Workers 编排
- [x] 0 网络 mock 规则引擎
- [x] record DTO + 紧凑构造器
- [ ] **生产补 1**:真实 LLM sub-agent 互调(FunctionToolCallback)
- [ ] **生产补 2**:PDF 输出(iText / OpenPDF)
- [ ] **生产补 3**:Google Calendar 集成写入行程

## 完整代码

[04-projects/project-3-travel-planner/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/04-projects/project-3-travel-planner)

## 学完下一步

[P4 AI 代码审查器 →](project-4-code-review.md) — @Tool 静态分析 + ChatClient 语义审查,PR review 自动化。
