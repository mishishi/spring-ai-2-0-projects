# 第 10 章 · Advanced RAG

> 🎯 目标:Spring AI 2.0 组合式 RAG(Multi-Query / Hybrid / Re-rank)

## 你将学到

- ✅ `MultiQueryExpander`(1 query → N query)
- ✅ `VectorStoreDocumentRetriever`(2.0 名字变了)
- ✅ `ConcatenationDocumentJoiner`(合并多路结果)
- ✅ `ContextualQueryAugmenter`(拼进 prompt)
- ✅ `RetrievalAugmentationAdvisor`(2.0 一行组合)

## 快速开始

```bash
cd 02-rag/10-advanced-rag
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

3 个 demo 对比:
- 基础 RAG(基线)
- Multi-Query RAG(1 → 3 检索)
- RetrievalAugmentationAdvisor 组合(2.0 新 API)

## 关键代码

### 1. 基础 RAG(基线)

```java
ChatClient client = builder
    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .build();
```

### 2. Multi-Query RAG(手动)

```java
MultiQueryExpander expander = MultiQueryExpander.builder()
    .chatClientBuilder(builder)
    .numberOfQueries(3)
    .build();
List<String> expanded = expander.expand("我工作满 6 年,能休几天假?");
// 3 个 query 各自 retrieve,合并,去重
```

### 3. RetrievalAugmentationAdvisor(2.0 组合式,推荐)

```java
RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
    .queryExpander(MultiQueryExpander.builder()
        .chatClientBuilder(builder)
        .numberOfQueries(3)
        .build())
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .topK(3)
        .similarityThreshold(0.5)
        .build())
    .documentJoiner(new ConcatenationDocumentJoiner())
    .queryAugmenter(ContextualQueryAugmenter.builder().build())
    .build();

ChatClient client = builder.defaultAdvisors(advisor).build();
```

## 4 个核心组件

| 组件 | 作用 | 2.0 实现 |
|---|---|---|
| QueryTransformer | 改写 / 翻译 / 压缩 query | `TranslationQueryTransformer` / `CompressionQueryTransformer` / `RewriteQueryTransformer` |
| QueryExpander | 扩展 query(N 个) | `MultiQueryExpander` |
| DocumentRetriever | 从 vector store / web / SQL 取 | `VectorStoreDocumentRetriever` |
| DocumentPostProcessor | 过滤 / 重排序 | `DocumentPostProcessor` (自定义 / Re-ranking) |
| DocumentJoiner | 合并多路结果 | `ConcatenationDocumentJoiner` |
| QueryAugmenter | 拼进 prompt | `ContextualQueryAugmenter` |

## 为什么需要 Multi-Query?

```
用户: "我工作满 6 年,能休几天假?"

基础 RAG:
  query → "工作满 6 年" + "年假"
  retrieve: 命中"满 5 年 15 天"
  
Multi-Query RAG:
  1. "我工作满 6 年,能休几天假?"
  2. "工龄 6 年的年假政策"
  3. "公司年假是怎么算的?"
  retrieve 3 轮: 命中"满 5 年 15 天" + "满 10 年 20 天" + "年假总数"
  → 更全,答案更准确
```

## 实战模式

| 场景 | 配置 |
|---|---|
| 简单 QA | 基础 RAG(1 query 检索) |
| 复杂问题 | Multi-Query(扩 3-5 个)+ Concatenation |
| 跨语言 | TranslationQueryTransformer(中 → 英检索) |
| 长对话 | CompressionQueryTransformer(压缩上下文) |
| 精确度 | Re-ranking(DocumentPostProcessor) |

## 完整代码

[02-rag/10-advanced-rag/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/10-advanced-rag)

## 下一章

[第 11 章 · Re-ranking →](11-reranking.md)

用 BGE / Cohere Rerank 提升 top-k 准确率
