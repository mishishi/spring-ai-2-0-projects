# 第 18 章 · Agent Production


## 你将学到

- ✅ MessageWindowChatMemory 滑动窗口
- ✅ Streaming 流式响应
- ✅ 安全 MathTool 防越界
- ✅ Actuator + Micrometer

## 快速开始

```bash
cd 03-agent/18-agent-production
export OPENAI_API_KEY=sk-xxxxx  # 可选, 0 网络下用 fake key 跑测试
mvn test                        # 0 网络测试
mvn spring-boot:run             # 真实跑
```

## 一句话总结

**ChatMemory + 流式 + 安全 + 监控**

## 关键 API

```java
// 核心 Pattern
ChatClient agent = builder.defaultTools(toolObject).build();
String answer = agent.prompt().user(query).call().content();
```

## 核心代码

```
03-agent/18-agent-production/src/main/java/
└── cc/misshi/springai/agentprod/StreamingController.java
└── cc/misshi/springai/agentprod/Application.java
└── cc/misshi/springai/agentprod/ChatMemoryConfig.java
└── cc/misshi/springai/agentprod/SafeMathTools.java
```

## 0 网络测试套路

```java
// 直接 new service, 传 null builder
MyService svc = new MyService(null, ...);
// service 内部 null-check, 走 mock 模板
```

## 学完下一步

读 [Phase 3 总览](overviews/phase-3.md),把 6 章串起来;或直接看 [Phase 4 项目实战](overviews/phase-4.md)。
