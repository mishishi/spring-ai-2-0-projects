# 第 13 章 · Agent Basics


## 你将学到

- ✅ ChatClient.prompt() 调 LLM
- ✅ @Tool 注解标记方法供 LLM 调用
- ✅ Agent loop: 用户 → LLM → tool → LLM → 用户
- ✅ Spring AI 2.0 ToolCallback API

## 快速开始

```bash
cd 03-agent/13-agent-basics
export OPENAI_API_KEY=sk-xxxxx  # 可选, 0 网络下用 fake key 跑测试
mvn test                        # 0 网络测试
mvn spring-boot:run             # 真实跑
```

## 一句话总结

**ChatClient + @Tool 起步 — 你的第一个 AI Agent**

## 关键 API

```java
// 核心 Pattern
ChatClient agent = builder.defaultTools(toolObject).build();
String answer = agent.prompt().user(query).call().content();
```

## 核心代码

```
03-agent/13-agent-basics/src/main/java/
└── cc/misshi/springai/agentbasics/CalculatorTools.java
└── cc/misshi/springai/agentbasics/AgentDemoController.java
└── cc/misshi/springai/agentbasics/Application.java
└── cc/misshi/springai/agentbasics/WeatherTools.java
```

## 0 网络测试套路

```java
// 直接 new service, 传 null builder
MyService svc = new MyService(null, ...);
// service 内部 null-check, 走 mock 模板
```

## 学完下一步

读 [Phase 3 总览](overviews/phase-3.md),把 6 章串起来;或直接看 [Phase 4 项目实战](overviews/phase-4.md)。
