# 第 7 章 · RAG Overview

> 🎯 目标:理解 RAG 4 步流程 + 跑通完整 in-memory RAG demo

## 你将学到

- ✅ RAG 是什么 + 为什么需要
- ✅ RAG 4 步流程:Load → Split → Embed → Store(然后 Retrieve)
- ✅ `SimpleVectorStore`(Spring AI 内置 in-memory)
- ✅ `QuestionAnswerAdvisor`(全自动 RAG advisor)

## 快速开始

```bash
cd 02-rag/07-rag-overview
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

跑起来会看到:
- 4 个公司制度文档加载 + 嵌入 + 存储
- 3 个 demo:手动 retrieve / 自动 RAG / 多问题

## RAG 4 步流程

```
┌─────────────┐    ┌─────────────┐    ┌──────────────┐    ┌──────────────┐
│ 1. LOAD     │ →  │ 2. SPLIT    │ →  │ 3. EMBED     │ →  │ 4. STORE     │
│ Document    │    │ TokenSplitter│    │ EmbeddingModel│    │ VectorStore  │
│ Reader      │    │ (切块)      │    │ (转向量)     │    │ (存向量)     │
└─────────────┘    └─────────────┘    └──────────────┘    └──────────────┘
                                                                │
                                                                ↓
┌─────────────┐    ┌──────────────┐    ┌──────────────┐
│ 4. ANSWER   │ ←  │ 3. AUGMENT   │ ←  │ 2. RETRIEVE  │ ← (用户提问)
│ LLM 生成    │    │ 拼进 prompt  │    │ 找 top-k 相似│
└─────────────┘    └──────────────┘    └──────────────┘
```

## 关键代码

### 1. 准备文档

```java
List<Document> documents = List.of(
    new Document("公司年假制度: 员工每年享有 10 天带薪年假..."),
    new Document("病假制度: 3 天以内无需医院证明..."),
    // ...
);
```

### 2. Embed + Store(自动)

```java
vectorStore.add(documents);
// SimpleVectorStore 内部:每个 Document → 调用 EmbeddingModel → 存内存 map
```

### 3. 手动 retrieve

```java
List<Document> top3 = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query("我年假有几天?")
        .topK(3)
        .build());
```

### 4. 全自动 RAG(QuestionAnswerAdvisor)

```java
ChatClient client = builder
    .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
    .build();

String reply = client.prompt()
    .user("我年假有几天?")
    .call()
    .content();
// QuestionAnswerAdvisor 自动:
// 1. 把 user message 作为 query,retrieve top-k 文档
// 2. 把文档拼进 system prompt
// 3. 让 LLM 基于文档回答
```

## 为什么需要 RAG?

| 问题 | 没 RAG | 有 RAG |
|---|---|---|
| 私有数据 | LLM 不可能知道 | 从你的文档库检索 |
| 实时数据 | 训练截止后的事不知道 | 检索时更新 |
| 幻觉 | 瞎编 | 基于真实文档 |
| 引用来源 | 不能 | retrieve 的 chunk 就是来源 |

## Vector Store 选型

| Store | 类型 | 何时用 |
|---|---|---|
| `SimpleVectorStore` | 内存(ConcurrentHashMap) | demo / 单元测试 |
| `PgVectorStore` | PostgreSQL + pgvector | 生产(标准方案,chapter 8) |
| `ChromaVectorStore` | Chroma server | 轻量 server |
| `MilvusVectorStore` | Milvus 分布式 | 大规模 |
| `RedisVectorStore` | Redis | 已有 Redis 基础设施 |
| `QdrantVectorStore` | Qdrant | 高性能 |

**生产推荐**:PgVector / Milvus / Qdrant。`SimpleVectorStore` 重启数据丢失。

## Embedding 模型选型

| Model | 维度 | 费用 | 何时用 |
|---|---|---|---|
| OpenAI `text-embedding-3-small` | 1536 | $0.02/1M tokens | 默认,质量高 |
| OpenAI `text-embedding-3-large` | 3072 | $0.13/1M tokens | 更高质量 |
| Ollama `nomic-embed-text` | 768 | 免费本地 | 隐私 / 离线 |
| BGE | 768 | 免费本地 | 中文场景 |

## 实战模式

| 场景 | 关键点 |
|---|---|
| 客服知识库 | 1000+ FAQ 文档 + `QuestionAnswerAdvisor` |
| 企业文档问答 | PDF/Word + `DocumentReader` + `PgVectorStore` |
| 代码助手 | 源代码 + 按目录 chunk + re-rank |
| 多模态 RAG | 图片 + 文本 + CLIP embedding |

## 测试

```bash
mvn test
```

0 网络(VectorStore 延迟初始化,context 启动不调嵌入 API)。

## 目录结构

```
07-rag-overview/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/ragoverview/
    │   │   └── Application.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/cc/misshi/springai/ragoverview/
            └── ApplicationTests.java
```

## 下一章

[第 8 章 · pgvector →](../08-pgvector/README.md)

把内存 SimpleVectorStore 换成 PostgreSQL pgvector,数据持久化 + 可扩展
