# 第 17 章 · Spring AI Graph


## 你将学到

- ✅ Graph / GraphNode 抽象
- ✅ addNode / addEdge / addConditionalEdge
- ✅ state machine 状态传递
- ✅ Review / Approval 流程示例

## 快速开始

```bash
cd 03-agent/17-spring-ai-graph
export OPENAI_API_KEY=sk-xxxxx  # 可选, 0 网络下用 fake key 跑测试
mvn test                        # 0 网络测试
mvn spring-boot:run             # 真实跑
```

## 一句话总结

**状态机 + 条件路由 — 复杂 Agent 流程**

## 关键 API

```java
// 核心 Pattern
ChatClient agent = builder.defaultTools(toolObject).build();
String answer = agent.prompt().user(query).call().content();
```

## 核心代码

```
03-agent/17-spring-ai-graph/src/main/java/
└── cc/misshi/springai/graph/Application.java
└── cc/misshi/springai/graph/Graph.java
└── cc/misshi/springai/graph/GraphNode.java
└── cc/misshi/springai/graph/GraphController.java
└── cc/misshi/springai/graph/ReviewGraphConfig.java
```

## 0 网络测试套路

```java
// 直接 new service, 传 null builder
MyService svc = new MyService(null, ...);
// service 内部 null-check, 走 mock 模板
```

## 学完下一步

读 [Phase 3 总览](overviews/phase-3.md),把 6 章串起来;或直接看 [Phase 4 项目实战](overviews/phase-4.md)。
