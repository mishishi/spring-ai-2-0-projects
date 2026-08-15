# 第 11 章 · Re-ranking 重排序


## 你将学到

- ✅ `DocumentPostProcessor` 接口(Spring AI 2.0 抽象)
- ✅ 关键词命中重排序(0 依赖,纯 Java)
- ✅ 集成到 `RetrievalAugmentationAdvisor`
- ✅ Cohere Rerank API(业界最准)
- ✅ BGE Reranker ONNX(本地 + 隐私)
- ✅ 3 种 Re-rank 方案对比 + 选型

## 一句话总结

Embedding 相似度 ≠ 真实相关性。`DocumentPostProcessor` 在 retriever 之后插入重排序步骤,把"最相关的" doc 推到 top-K 前面 — Cohere API 业界最准,BGE 本地最强,关键词 0 依赖最简单。

## 读者学完能做什么

- 理解 re-rank 解决的问题(相似度 ≠ 相关性)
- 写自定义 DocumentPostProcessor
- 用关键词命中重排序(纯 Java,0 依赖)
- 集成 Cohere Rerank(云 API)
- 集成 BGE Reranker(本地 ONNX)
- 按场景选 re-rank 方案

## 5 分钟上手

```bash
cd 02-rag/11-reranking
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

3 个 demo 对比:
- 基线(按 embedding 相似度,可能不准确)
- 关键词 Re-rank(重排序)
- 完整 RAG 集成 re-rank

## 为什么需要 Re-rank(背景)

ch10 的 Multi-Query 解决了**召回**问题(找全),但还有一个**排序**问题没解决:

```
Query: "年假怎么算?"

Vector search top-4(按相似度):
  1. 病假制度   ← 相似度 0.82 (但答非所问)
  2. 年假制度   ← 相似度 0.80 (相关)
  3. 加班制度   ← 相似度 0.71 (相似度中等)
  4. 报销制度   ← 相似度 0.60 (相似度低)

Re-rank 后:
  1. 年假制度   ← 关键词命中,排第一 ✓
  2. 病假制度
  3. 加班制度
  4. 报销制度
```

**embedding 相似度 ≠ 真实相关性**。Re-rank 用专门模型(Cohere / BGE)或启发式(关键词)**二次排序**。

**类比**:
- Vector search = Google 搜索(粗排,百万级召回)
- Re-rank = Google 的 PageRank(精排,top 10 排名)

## 关键概念(4 个)

### 概念 1:`DocumentPostProcessor` 接口

Spring AI 2.0 抽象的统一 re-rank 接口:

```java
@FunctionalInterface
public interface DocumentPostProcessor {
    List<Document> process(Query query, List<Document> documents);
}
```

**实现自由**:
- 关键词命中(纯 Java,0 依赖)
- Cohere Rerank(调云 API)
- BGE Reranker(本地 ONNX 模型)
- 任何自定义算法

### 概念 2:关键词命中 Re-rank(0 依赖)

**原理**:query 拆词,每个 doc 算"关键词命中次数",按命中数重排。

```java
@Component
public class KeywordRerankProcessor implements DocumentPostProcessor {
    @Override
    public List<Document> process(Query query, List<Document> documents) {
        String[] keywords = query.text().toLowerCase().split("\\s+");
        return documents.stream()
            .sorted((a, b) -> Double.compare(score(b, keywords), score(a, keywords)))
            .toList();
    }

    private double score(Document doc, String[] keywords) {
        String text = doc.getText().toLowerCase();
        int hits = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) hits++;
        }
        return hits;
    }
}
```

**优点**:0 依赖,0 成本,5 分钟写完。
**缺点**:不懂语义("car" 不会匹配 "automobile")。

### 概念 3:Cohere Rerank(业界最准)

调 Cohere 云 API:

```java
@Component
public class CohereRerankProcessor implements DocumentPostProcessor {
    @Override
    public List<Document> process(Query query, List<Document> documents) {
        CohereClient cohere = CohereClient.builder()
            .apiKey(System.getenv("COHERE_API_KEY"))
            .build();

        RerankResponse response = cohere.rerank(
            RerankRequest.builder()
                .query(query.text())
                .documents(documents.stream().map(Document::getText).toList())
                .topN(3)               // 取前 3
                .model("rerank-english-v2.0")
                .build());

        // 按 Cohere score 重排
        return response.getResults().stream()
            .map(r -> documents.get(r.getIndex()))
            .toList();
    }
}
```

**准确度**:业界 SOTA,英文最强(中文需用 `rerank-multilingual-v2.0`)。
**成本**:$1/1000 search。
**延迟**:~200ms。

### 概念 4:BGE Reranker(本地 ONNX)

`BAAI/bge-reranker-v2-m3` 模型 + ONNX runtime 跑本地:

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.16.0</version>
</dependency>
```

```java
BgeReranker reranker = BgeReranker.fromPretrained("BAAI/bge-reranker-v2-m3");
// 调 reranker.compute(query, docs) → score
```

**优点**:本地 + 免费 + 准确度接近 Cohere。
**缺点**:模型 ~1GB 首次下载,GPU 加速才有速度。

## 4 个实战场景

### 场景 1:基线(无 re-rank,对比)

```java
RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .topK(4)
        .build())
    // 不加 documentPostProcessor
    .build();
```

### 场景 2:关键词 Re-rank(推荐入门)

```java
RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .topK(5)                  // 先取 5 个
        .build())
    .documentPostProcessor(new KeywordRerankProcessor())   // ← 加这一行
    .build();
```

**5 → 3**:取 5 个,re-rank 后留 3 个。

### 场景 3:Cohere Rerank(生产推荐)

```java
RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .topK(10)                 // 先取 10 个(粗排)
        .build())
    .documentPostProcessor(new CohereRerankProcessor())    // ← Cohere 精排
    .build();
```

**10 → 3**:粗排 10,精排 3。

### 场景 4:BGE Reranker(本地 + 隐私)

```java
RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .topK(10)
        .build())
    .documentPostProcessor(new BgeRerankProcessor())      // ← BGE 精排
    .build();
```

## 完整 Pipeline 流程图

```
query
  ↓
[QueryExpander]  ← 扩 N 个
  ↓
[DocumentRetriever]  ← 取 top-K(粗排,5-10 个)
  ↓
[DocumentPostProcessor]  ← Re-rank(关键!)
  ↓                          ↑
[DocumentJoiner]           Cohere / BGE / 关键词
  ↓
[QueryAugmenter]  ← 拼进 prompt
  ↓
LLM call
```

## 3 种 Re-rank 方案对比

| 方案 | 优 | 劣 | 成本 | 推荐 |
|---|---|---|---|---|
| **关键词命中**(本章 demo) | 0 依赖,简单 | 不懂语义 | 0 | 学习 / Demo |
| **Cohere Rerank API** | 业界最准,云 API | 需 API key + 付费 | $1/1000 search | 生产(预算够) |
| **BGE Reranker (ONNX)** | 本地,免费,准确度高 | 需下载模型 ~1GB | 0 | 生产(隐私) |

**选型决策**:

```
要本地 + 隐私?
├── 是 → BGE Reranker
└── 否 → 预算够?
    ├── 是 → Cohere Rerank
    └── 否 → 关键词(简单场景) / BGE(复杂)
```

## 关键词 Re-rank 调优

### 关键词提取优化

```java
// 简单 split 不好:中文不分词
String text = "我工作满 6 年,能休几天假?";
String[] kw = text.split("\\s+");   // ["我工作满", "6", "年,能休几天假?"]  ← 乱

// 应该用 HanLP / Jieba 分词(中文)
// 或简单:按字符 2-gram
List<String> grams = extractBigrams(text);
// ["我工", "工作", "作满", "满 ", " 6", ...]
```

### 加权策略

```java
// TF-IDF 风格:稀有词权重高
private double score(Document doc, String[] keywords) {
    Map<String, Integer> freq = new HashMap<>();
    for (String kw : keywords) freq.merge(kw, 1, Integer::sum);
    return freq.values().stream().mapToInt(Integer::intValue).sum();
}
```

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class KeywordRerankProcessorTest {
    @Autowired DocumentPostProcessor rerankProcessor;

    @Test
    void testRerank() {
        List<Document> docs = List.of(
            new Document("病假制度"),     // 跟 query 不太相关
            new Document("年假制度"),     // 跟 query 完全相关
            new Document("加班制度")
        );
        Query q = new Query("年假");
        List<Document> reranked = rerankProcessor.process(q, docs);
        assertThat(reranked.get(0).getText()).contains("年假");  // 排第一
    }
}
```

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| 关键词 Re-rank 中文乱 | "我工作" 当一个词 | 用 jieba / HanLP / 2-gram |
| Cohere 中文差 | 准确度低 | 换 `rerank-multilingual-v2.0` |
| BGE 模型 1GB 首次下载 | 启动慢 | 预下载到镜像,或者用 GPU |
| Re-rank 慢(>1s) | 用户等待 | 异步化 / 限制 top-K |
| Retriever topK=20 + rerank | 浪费 token | topK=5-10 够用 |
| 关键词不区分大小写 | "Java" / "java" 不匹配 | `.toLowerCase()` + `Locale.ROOT` |
| 同一关键词重复出现算多次 | 分数膨胀 | 用 set 而不是 list |
| Re-rank 后 doc 全空(过滤太严) | LLM 答非所问 | 降 threshold / 加 fallback |

## 实战部署清单

- [ ] 选 Re-rank 方案(关键词 / Cohere / BGE)
- [ ] 实现 `DocumentPostProcessor` 接口
- [ ] Retriever topK=5-10(粗排)
- [ ] Re-rank 后留 top-3(精排)
- [ ] 集成到 `RetrievalAugmentationAdvisor.documentPostProcessor()`
- [ ] 对比 re-rank 前后 top-3 准确度
- [ ] 监控:re-rank 延迟 / API 调用次数
- [ ] 兜底:re-rank 失败时 fallback 到原始 retriever 顺序
- [ ] `mvn test` 验证关键词匹配

## 完整代码

[02-rag/11-reranking/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/11-reranking)

## 下一步

- [第 12 章 · RAG Production →](12-rag-production.md)— 增量更新 / 缓存 / 监控,生产级 RAG
- [第 18 章 · Agent Production →](19-agent-production.md)— Agent 场景的 RAG 实战
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
