# 第 7 章 · RAG Overview

> Phase 2 · RAG 实战
> 🎯 理解 RAG 4 步流程 + 跑通 in-memory RAG demo

## 一句话总结

RAG = 让 LLM 基于你的私有文档回答,核心 4 步:Load → Split → Embed → Store,提问时 Retrieve top-k → 拼进 prompt → LLM 答。

## 读者学完能做什么

- 理解 RAG 4 步流程
- 用 SimpleVectorStore 跑 RAG demo
- 用 QuestionAnswerAdvisor 自动化 RAG
- 知道什么时候用哪种 Vector Store

## RAG 4 步详解

### Step 1: Load(文档加载)

```java
new Document("公司年假制度: 10 天带薪年假...")
// 或者:
FileSystemDocumentReader reader = new FileSystemDocumentReader(...);
List<Document> docs = reader.read();
```

**DocumentReader 实现**:
- `PagePdfDocumentReader` — PDF
- `TikaDocumentReader`(Apache Tika)— Word/PPT/HTML
- `MarkdownDocumentReader` — Markdown
- `JsonReader` — JSON

### Step 2: Split(切块)

LLM context 有上限(8K/32K/128K),文档太长要切。

```java
TokenTextSplitter splitter = new TokenTextSplitter(500);  // 每块 500 tokens
List<Document> chunks = splitter.split(documents);
```

**主流 splitter**:
- `TokenTextSplitter` — 按 token 数切(推荐)
- `RecursiveCharacterTextSplitter` — 按段落/句子切
- `SemanticChunker` — 按语义切(高级)

### Step 3: Embed(嵌入)

每个 chunk → EmbeddingModel → 高维向量(768/1536/3072 维)。

```java
EmbeddingModel embeddingModel = ...;  // Spring AI 自动注入
float[] vector = embeddingModel.embed("Spring AI 是什么?");
// vector.length = 1536 (OpenAI text-embedding-3-small)
```

### Step 4: Store(存储)

向量 + 元数据存到 VectorStore。

```java
vectorStore.add(documents);
// 内部自动 embed + 存
```

### Step 2': Retrieve(查询时)

用户问 → 嵌入 → cosine similarity → top-k 文档。

```java
List<Document> topK = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(userQuestion)
        .topK(5)
        .similarityThreshold(0.7)  // 过滤低相似度
        .build()
);
```

## QuestionAnswerAdvisor 内部流程

```
user: "我年假有几天?"
    ↓
QuestionAnswerAdvisor.before():
  1. 把 user message 作为 query
  2. 调用 vectorStore.similaritySearch(query, topK=2)
  3. 把 retrieve 的 documents 拼到 system prompt:
     "Use the following context to answer:
      <context>
      Doc1: 公司年假制度: 员工每年享有 10 天...
      Doc2: 年假需提前 3 天申请...
      </context>"
    ↓
LLM call(带 context 的 prompt)
    ↓
QuestionAnswerAdvisor.after():
  返回 LLM 回答
```

## SimpleVectorStore vs 生产 VectorStore

| 维度 | SimpleVectorStore | PgVectorStore |
|---|---|---|
| 持久化 | ❌ 重启丢 | ✅ 数据库 |
| 容量 | MB 级 | GB / TB |
| 多实例 | ❌ | ✅ 共享 |
| 启动 | 0 配置 | 需 pgvector extension |
| 性能 | 快(内存) | 慢但稳 |

**用法对比**:
```java
// SimpleVectorStore
@Bean
VectorStore simpleVectorStore(EmbeddingModel model) {
    return new SimpleVectorStore(model);
}

// PgVectorStore(配置好后自动注入)
@Bean
VectorStore pgVectorStore(JdbcTemplate jdbc, EmbeddingModel model) {
    return new PgVectorStore(jdbc, model);
}
```

## 完整代码

[02-rag/07-rag-overview/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/07-rag-overview)

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| 文档没切块,超 LLM 上下文 | 报错或截断 | 用 TokenTextSplitter |
| 嵌入维度不匹配 | 检索 0 结果 | 全程用同一 EmbeddingModel |
| 中文 RAG 效果差 | 答非所问 | 用 BGE / M3E 中文 embedding |
| retrieve 的 doc 跟问题无关 | LLM 答偏 | 调 topK / 加 re-rank(chapter 11) |
| SimpleVectorStore 重启数据丢 | 生产事故 | 用 PgVector / Milvus |

## 下一步

- [第 8 章 · pgvector →](../02-rag/08-pgvector/README.md)
- 把 SimpleVectorStore 换成真实 PostgreSQL pgvector

**chapter 8 需要你拍 1 个 P0 决策**:
- A1. 本地 Docker pgvector(标准方案)
- A2. 用 SQLite + sqlite-vss(轻量,单文件)
- A3. 跳过 chapter 8,直接用 SimpleVectorStore 进 chapter 9
