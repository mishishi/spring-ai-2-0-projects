# 2026-08-11 Chapter 7 - RAG Overview + 升 Spring AI 2.0.0

## 背景

Phase 2 第 7 章,跑通 RAG 完整 demo。但发现 1.1.3 不支持完整 RAG,需要升 2.0.0。

## 决策

### D-16:Spring AI 1.1.3 → 2.0.0(GA)+ Spring Boot 3.4.0 → 4.0.0

**触发原因**:
- ch7 准备用 `VectorStore` / `SimpleVectorStore` / `QuestionAnswerAdvisor`,1.1.3 完全没有
- Spring AI 2.0.0 GA 实际已发布(Maven Central 有 2.0.0),D-6 降级决定基于 6 月过时的 web search
- 2.0.0 在 Java 17 也能跑(虽然官方推荐 Java 21)

**试错过程**:
1. 升 `spring-ai.version: 1.1.3 → 2.0.0`(保持 Spring Boot 3.4.0)→ 失败:reactor-netty 不兼容
2. 升 Spring Boot 3.4.0 → 3.5.0 → 失败:Jackson bean 重复定义
3. 升 Spring Boot → 4.0.0 → ✅ 成功,chapter 1-6 不改代码直接过

**最终 stack**:Java 17 + Spring Boot 4.0.0 + Spring AI 2.0.0(实际跟 D-5 + D-6 的"原计划"一致!)

### D-17:Spring AI 2.0.0 RAG 新 API

**变化**:
- 1.1.3: `QuestionAnswerAdvisor` 在 `org.springframework.ai.chat.client.advisor.qa`
- 2.0.0: `QuestionAnswerAdvisor` 移到 `org.springframework.ai.chat.client.advisor.vectorstore`,**包路径变了**
- 2.0.0 新模块 `spring-ai-rag` 提供 `RetrievalAugmentationAdvisor`(可组合 transformer / retriever / postprocessor,更强大)
- 2.0.0 builder 模式:`QuestionAnswerAdvisor.builder(vectorStore).build()`(不是 `new QuestionAnswerAdvisor(vectorStore)`)
- 2.0.0 `Document.getContent()` → `getText()`

**实际改的代码**(ch7 only):
```java
// import
- import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
+ import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

// Document
- d.getContent()
+ d.getText()

// Advisor 构造
- new QuestionAnswerAdvisor(vectorStore)
+ QuestionAnswerAdvisor.builder(vectorStore).build()
```

**Chapter 1-6 代码 0 改动,直接过**(API 兼容)。

## 实际产出

### 文件清单

- [x] `02-rag/07-rag-overview/pom.xml`
- [x] `02-rag/07-rag-overview/src/main/java/.../Application.java`
- [x] `02-rag/07-rag-overview/src/main/resources/application.yml`
- [x] `02-rag/07-rag-overview/src/test/java/.../ApplicationTests.java`
- [x] `02-rag/07-rag-overview/README.md`
- [x] `docs/07-rag-overview.md`
- [x] `02-rag/08-pgvector/.gitkeep` 等 5 个空目录骨架
- [x] `pom.xml`(根:升级 Spring Boot 3.4.0 → 4.0.0 + Spring AI 1.1.3 → 2.0.0)

### 验证结果

| 步骤 | 耗时 | 结果 |
|---|---|---|
| `mvn -pl 02-rag/07-rag-overview -am test` | 1:01 min(首次 build) | ✅ Tests run 1 |
| **`mvn test`(全 7 module)** | **29.5s** | ✅ **全 SUCCESS** |

## Git 状态

- **Commit**:Phase 2 第 7 章 + 升级
- **Push**:`a6097f7..xxx main -> main` ✅
- **Bundle**:`/Users/zhurenbao/Documents/spring-ai-2-0-projects-20260811-1809-chapter-7.bundle`

## 升级栈总结

| 维度 | 原(D-6) | 实际(D-16) | 备注 |
|---|---|---|---|
| Spring AI | 1.1.3 | **2.0.0** | RAG API 完整 |
| Spring Boot | 3.4.0 | **4.0.0** | 2.0 强制 3.5+/4.0+ |
| Java | 17 | 17 ✅ | 2.0 推荐 21,但 17 能跑 |

## 下一章(待决策)

**Chapter 8: pgvector** — 需要你拍 P0:

- **A1. 本地 Docker pgvector**(标准方案,生产级)
- **A2. SQLite + sqlite-vss**(轻量,单文件,适合教学)
- **A3. Chroma**(独立 server,Docker 一键起)
- **A4. 暂时跳过 ch8,继续 ch9 (Document Loaders)**,用 SimpleVectorStore 继续
