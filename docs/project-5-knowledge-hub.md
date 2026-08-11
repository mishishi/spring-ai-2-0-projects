# P5 · AI 综合知识中心


## 你将学到

- ✅ 完整 Spring Boot 项目骨架(MVC / Service / DTO)
- ✅ Spring AI 2.0 ChatClient 真实用法
- ✅ **0 网络可测** — 模板/mock 模式
- ✅ Maven 单模块独立可跑

## 快速开始

```bash
cd 04-projects/project-5-knowledge-hub
export OPENAI_API_KEY=sk-xxxxx  # 可选
mvn test                        # 0 网络测试
mvn spring-boot:run             # 真实跑
```

## 一句话总结

**RAG + Tool + Memory + Multi-Agent 一体化**

## 核心代码

```
04-projects/project-5-knowledge-hub/src/main/java/
└── cc/misshi/springai/knowledgehub/KnowledgeHubController.java
└── cc/misshi/springai/knowledgehub/Application.java
└── cc/misshi/springai/knowledgehub/KnowledgeBase.java
└── cc/misshi/springai/knowledgehub/CodeSnippetAnalyzer.java
└── cc/misshi/springai/knowledgehub/KnowledgeHubService.java
└── cc/misshi/springai/knowledgehub/QueryRouter.java
└── cc/misshi/springai/knowledgehub/ConversationMemory.java
```

## 学完下一步

读 [Phase 4 总览](overviews/phase-4.md),把 5 个项目串起来。
