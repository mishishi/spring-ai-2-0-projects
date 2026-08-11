# Phase 2 · Spring AI 2.0 RAG 实战

<div class="editorial-section-label">

Spring AI 2.0 · 20 周项目实战

</div>

## 6 章搞懂 RAG 端到端 (Indexing → Retrieval → Generation)

> **pgvector 真实持久化 + Multi-Query + Re-ranking + 生产级 3 大支柱**。每个 chapter 配独立可跑的 Spring Boot 项目,集成测试用真实 pgvector(Docker 5433 端口避开本地 PG)。

------------------------------------------------------------------------

<div class="phase-cta">

<a href="../07-rag-overview.md" class="editorial-cta">▶ 开始阅读 Phase 2</a> <a href="../08-pgvector.md" class="editorial-cta editorial-cta--ghost">↓ 直接看 pgvector</a>

</div>

## TL;DR

**RAG (Retrieval-Augmented Generation) = 让 LLM 基于你的私有文档回答**。Spring AI 2.0 提供完整 4 步 pipeline + 组合式 advisor (`RetrievalAugmentationAdvisor`)。Phase 2 从 in-memory (SimpleVectorStore) 演进到真实持久化 (pgvector) 到生产级 (缓存 / 监控 / 增量)。

**3 个 take-away:**
1. **4 步 pipeline**: Load → Split → Embed → Store,每步一个 spring-ai starter
2. **pgvector 真持久化**: HNSW 索引 / COSINE 距离 / Docker 一键起
3. **2.0 组合式 RAG**: `RetrievalAugmentationAdvisor` 拼装 queryExpander + documentRetriever + documentJoiner + documentPostProcessors

------------------------------------------------------------------------

## 6 章目录

| # | 章节 | 主题 | 关键 API |
|----|----|----|----|
| 07 | [RAG Overview](../07-rag-overview.md) | 4 步流程 + SimpleVectorStore | `VectorStore.add()` / `QuestionAnswerAdvisor` |
| 08 | [pgvector](../08-pgvector.md) | 真实持久化 / HNSW / 维度 | `PgVectorStore.builder().dimensions()` |
| 09 | [Document Loaders](../09-document-loaders.md) | MD / HTML / PDF + 切块 | `MarkdownDocumentReader` / `TokenTextSplitter` |
| 10 | [Advanced RAG](../10-advanced-rag.md) | Multi-Query / 2.0 组合式 | `MultiQueryExpander` / `RetrievalAugmentationAdvisor` |
| 11 | [Re-ranking](../11-reranking.md) | DocumentPostProcessor + 关键词 | `DocumentPostProcessor` / Cohere / BGE |
| 12 | [RAG Production](../12-rag-production.md) | 增量 / Caffeine / Actuator | `@Cacheable` / Micrometer |

<div class="editorial-stats">

<div class="editorial-stat">

<span class="editorial-stat__num">6</span><span class="editorial-stat__label">章</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">4</span><span class="editorial-stat__label">项目</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num editorial-stat__num--accent">~30s</span><span class="editorial-stat__label">mvn test</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">0</span><span class="editorial-stat__label">网络</span>

</div>

<div class="editorial-stat">

<span class="editorial-stat__num">1</span><span class="editorial-stat__label">pgvector</span>

</div>

</div>

------------------------------------------------------------------------

## 6 个核心模块(项目代码)

```
02-rag/07-rag-overview/src/main/java/.../
└── Application.java   # SimpleVectorStore + QuestionAnswerAdvisor

02-rag/08-pgvector/src/main/java/.../
└── Application.java   # PgVectorStore + 5433 Docker

02-rag/09-document-loaders/src/main/java/.../
└── Application.java   # MarkdownReader + TikaReader + TokenTextSplitter

02-rag/10-advanced-rag/src/main/java/.../
└── Application.java   # MultiQueryExpander + RetrievalAugmentationAdvisor

02-rag/11-reranking/src/main/java/.../
├── Application.java
└── KeywordRerankProcessor.java   # DocumentPostProcessor 实现

02-rag/12-rag-production/src/main/java/.../
└── Application.java   # @Cacheable + Actuator + 增量更新
```

------------------------------------------------------------------------

## 关键技术点(Spring AI 2.0 RAG 新架构)

| 维度 | 1.x | **2.0** |
|----|----|----|
| Vector Store | `QuestionAnswerAdvisor`(qa 包) | `QuestionAnswerAdvisor`(vectorstore 包,builder 模式) |
| RAG 组合 | 单个 advisor | **`RetrievalAugmentationAdvisor`(可组合 4 段)** |
| Multi-Query | 无 | `MultiQueryExpander` |
| Document Joiner | 无 | `ConcatenationDocumentJoiner` |
| Document Post-Processor | 无 | **`DocumentPostProcessor` 抽象** |
| Query Augmenter | 无 | `ContextualQueryAugmenter` |
| Re-ranking | 手写 | 抽象接口 + 关键词 / Cohere / BGE 实现 |

------------------------------------------------------------------------

## pgvector Docker 启动

```bash
docker run -d --name pgvector -p 5433:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=vectordb \
  pgvector/pgvector:pg16
```

> **端口 5433 原因**: macOS 自带 PostgreSQL 占 5432,Docker 用 5433 避开。

------------------------------------------------------------------------

## 4 步 RAG Pipeline

```
[DocumentReader] → [DocumentSplitter] → [EmbeddingModel] → [VectorStore]
        ↓                  ↓                  ↓              ↓
       PDF/MD/HTML      TokenTextSplitter   text-embedding  pgvector
                                                  -3-small
                                                          
[Query] → [Retriever] → [Rerank] → [Augment] → [LLM call]
            ↓            ↓          ↓
         top-K       sort       拼进 system prompt
```

------------------------------------------------------------------------

## 快速开始

```bash
# 1. 起 pgvector
docker run -d --name pgvector -p 5433:5432 \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=vectordb \
  pgvector/pgvector:pg16

# 2. 跑 Phase 2 第一个 chapter
cd 02-rag/07-rag-overview
mvn test
mvn spring-boot:run   # (需要 OPENAI_API_KEY)
```

------------------------------------------------------------------------

<div class="editorial-section-label">

下一步

</div>

[**07 RAG Overview** →](../07-rag-overview.md)  ·  [Phase 1 基础 ←](phase-1.md)  ·  [Phase 3 Agent + 项目(待开始)](../index.md)
