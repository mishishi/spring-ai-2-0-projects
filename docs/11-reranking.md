# 第 11 章 · Re-ranking


## 你将学到

- ✅ `DocumentPostProcessor` 接口(2.0 抽象)
- ✅ 自定义 `KeywordRerankProcessor`(关键词命中加权)
- ✅ 集成到 `RetrievalAugmentationAdvisor` 的 `documentPostProcessor` 钩子
- ✅ 生产用 Cohere Rerank / BGE Reranker(简介)

## 快速开始

```bash
cd 02-rag/11-reranking
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

3 个 demo 对比:
- 基线(按 embedding 相似度,可能不准确)
- 关键词 Re-rank(重排序)
- 完整 RAG 集成 re-rank(2.0 advisor)

## 关键代码

### 1. 自定义 DocumentPostProcessor

```java
@Component
public class KeywordRerankProcessor implements DocumentPostProcessor {
    @Override
    public List<Document> process(Query query, List<Document> documents) {
        String[] keywords = query.text().toLowerCase().split("\\s+");
        // 算每个 doc 的关键词命中分数
        // 按分数降序排
        return documents.stream()
            .sorted((a, b) -> Double.compare(score(b, keywords), score(a, keywords)))
            .toList();
    }
}
```

### 2. 集成到 RetrievalAugmentationAdvisor

```java
RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .topK(5)
        .build())
    .documentPostProcessor(rerankProcessor)  // ← 加这一行
    .build();
```

### 3. 流程图

```
query
  ↓
[QueryExpander]  ← 扩 N 个
  ↓
[DocumentRetriever]  ← 取 top-K
  ↓
[DocumentPostProcessor]  ← Re-rank(关键!)
  ↓
[DocumentJoiner]  ← 合并多路
  ↓
[QueryAugmenter]  ← 拼进 prompt
  ↓
LLM call
```

## 为什么需要 Re-rank?

```
Query: "年假怎么算?"

Vector search top-4:
  1. 病假制度  ← 相似度高但答非所问
  2. 年假制度  ← 相关
  3. 加班制度  ← 相似度中等
  4. 报销制度  ← 相似度低

Re-rank 后:
  1. 年假制度  ← 关键词命中,排第一
  2. 病假制度
  3. 加班制度
  4. 报销制度
```

**embedding 相似度 ≠ 真实相关性**。Re-rank 用专门模型(Cohere / BGE)或启发式(关键词)二次排序。

## 3 种 Re-rank 方案

| 方案 | 优 | 劣 | 推荐 |
|---|---|---|---|
| **关键词命中**(本章 demo) | 0 依赖,简单 | 不懂语义 | 学习 |
| **Cohere Rerank API** | 业界最准,云 API | 需 API key + 付费 | 生产 |
| **BGE Reranker (ONNX)** | 本地,免费,准确度高 | 需下载模型 ~1GB | 生产(隐私) |

### Cohere Rerank(推荐)

```java
@Component
public class CohereRerankProcessor implements DocumentPostProcessor {
    @Override
    public List<Document> process(Query query, List<Document> documents) {
        // 调 Cohere Rerank API
        CohereClient cohere = CohereClient.builder()
            .apiKey(System.getenv("COHERE_API_KEY"))
            .build();
        RerankResponse response = cohere.rerank(
            RerankRequest.builder()
                .query(query.text())
                .documents(documents.stream().map(Document::getText).toList())
                .topN(3)
                .build());
        // 按 Cohere score 重排
        // ...
    }
}
```

### BGE Reranker(本地 ONNX)

```xml
<dependency>
    <groupId>com.huggingface</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.16.0</version>
</dependency>
```

```java
BgeReranker reranker = BgeReranker.fromPretrained("BAAI/bge-reranker-v2-m3");
// 调 reranker.compute(query, docs) → score
```

## 实战模式

| 场景 | 配置 |
|---|---|
| 学习 / Demo | KeywordRerankProcessor(本章) |
| 生产(预算够) | Cohere Rerank |
| 生产(隐私 / 离线) | BGE Reranker ONNX |
| 不需要(简单 FAQ) | 不加 re-rank |

## 完整代码

[02-rag/11-reranking/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/11-reranking)

## 下一章

[第 12 章 · RAG Production →](12-rag-production.md)

增量更新 / 缓存 / 监控,生产级 RAG 系统
