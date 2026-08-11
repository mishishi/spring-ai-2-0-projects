# P3 · AI 旅行规划师


## 你将学到

- ✅ 完整 Spring Boot 项目骨架(MVC / Service / DTO)
- ✅ Spring AI 2.0 ChatClient 真实用法
- ✅ **0 网络可测** — 模板/mock 模式
- ✅ Maven 单模块独立可跑

## 快速开始

```bash
cd 04-projects/project-3-travel-planner
export OPENAI_API_KEY=sk-xxxxx  # 可选
mvn test                        # 0 网络测试
mvn spring-boot:run             # 真实跑
```

## 一句话总结

**Multi-Agent 编排 — 4 个 sub-agent + 1 orchestrator**

## 核心代码

```
04-projects/project-3-travel-planner/src/main/java/
└── cc/misshi/springai/travelplanner/Application.java
└── cc/misshi/springai/travelplanner/TravelPlannerController.java
└── cc/misshi/springai/travelplanner/TravelAgentsConfig.java
└── cc/misshi/springai/travelplanner/TravelPlannerService.java
```

## 学完下一步

读 [Phase 4 总览](overviews/phase-4.md),把 5 个项目串起来。
