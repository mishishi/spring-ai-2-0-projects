# 第 10 章 · Advanced RAG 高级检索


## 你将学到

- ✅ `RetrievalAugmentationAdvisor` — Spring AI 2.0 组合式 RAG 框架
- ✅ `MultiQueryExpander` — 1 query 扩 N 个,提升召回
- ✅ `VectorStoreDocumentRetriever` — 2.0 改名后的统一 retriever
- ✅ `ConcatenationDocumentJoiner` — 合并多路检索结果
- ✅ `ContextualQueryAugmenter` — 自动拼进 prompt
- ✅ 4 种 QueryTransformer:Rewrite / Translation / Compression / Custom
- ✅ Multi-Query 完整 pipeline + 实战模式

## 一句话总结

`RetrievalAugmentationAdvisor.builder()` 把 **QueryExpander + DocumentRetriever + DocumentPostProcessor + DocumentJoiner + QueryAugmenter** 像乐高一样组合,一行写完整 RAG 流水线。

## 读者学完能做什么

- 用 RetrievalAugmentationAdvisor 组合复杂 RAG
- 选 QueryExpander(扩 query) / Retriever(取 doc) / Joiner(合并) / Augmenter(拼 prompt)
- 用 Multi-Query 提升召回率(尤其模糊 query)
- 跨语言检索(中文 query → 英文 docs)
- 长对话压缩(节省 token)
- 写自定义 QueryTransformer

## 5 分钟上手

```bash
cd 02-rag/10-advanced-rag
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

3 个 demo 对比:
- 基础 RAG(基线)
- Multi-Query RAG(1 → 3 检索)
- RetrievalAugmentationAdvisor 组合(2.0 新 API)

## 为什么需要 Advanced RAG(背景)

ch7-9 是基础 RAG(1 query → 1 检索 → 1 答案),生产里 3 个痛点:

**痛点 1:模糊 query 召回差**
```
用户: "我工作满 6 年,能休几天假?"
基础 RAG: 命中"满 5 年 15 天"(勉强)
真实意图: 用户想算"满 5/10/20 年分别几天" → 需要多 query
```

**痛点 2:跨语言检索差**
```
中文 docs + 英文 query → 检索命中率 30%
中文 query + 英文 docs → 检索命中率 50%
→ 翻译 query 到跟 docs 同一语言,提升 50%+
```

**痛点 3:长对话 token 爆**
```
对话历史 5000 token + RAG 1000 token + system 200 token
→ 超 6K,接近上下文上限
→ 压缩历史到 500 token
```

**Advanced RAG 解决**:在 query 进入 vector store 之前 / 之后插入多个处理步骤,精细化控制。

## 关键概念(4 个 RAG 组件)

`RetrievalAugmentationAdvisor` 内部有 5 个钩子:

| 组件 | 作用 | 2.0 实现 |
|---|---|---|
| **QueryTransformer** | 改写 / 翻译 / 压缩 query | `TranslationQueryTransformer` / `CompressionQueryTransformer` / `RewriteQueryTransformer` |
| **QueryExpander** | 扩展 query(N 个) | `MultiQueryExpander` |
| **DocumentRetriever** | 从 vector store / web / SQL 取 | `VectorStoreDocumentRetriever` |
| **DocumentPostProcessor** | 过滤 / 重排序 | 自定义 / Re-ranking(下一章) |
| **DocumentJoiner** | 合并多路结果 | `ConcatenationDocumentJoiner` |
| **QueryAugmenter** | 拼进 prompt | `ContextualQueryAugmenter` |

## 4 个实战场景

### 场景 1:基础 RAG(基线)

```java
ChatClient client = builder
    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .build();
```

跟 ch7-9 一模一样,作为对比基线。

### 场景 2:Multi-Query RAG(1 query → 3 query)

```java
MultiQueryExpander expander = MultiQueryExpander.builder()
    .chatClientBuilder(builder)
    .numberOfQueries(3)              // 扩成 3 个
    .build();

List<String> expanded = expander.expand("我工作满 6 年,能休几天假?");
// → ["我工作满 6 年,能休几天假?", "工龄 6 年的年假政策", "公司年假是怎么算的?"]
//   ↑ LLM 自动改写,3 个不同角度
```

3 个 query 各自 retrieve → 合并去重 → top-K。**召回率从 60% 提升到 85%+**。

### 场景 3:RetrievalAugmentationAdvisor 完整组合(2.0 推荐)

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

**这一段代码 = Multi-Query + Retriever + Joiner + Augmenter 全套**。一气呵成。

### 场景 4:QueryTransformer 改写(翻译)

```java
// 中文 query → 英文 query → 英文 docs 命中
TranslationQueryTransformer transformer = TranslationQueryTransformer.builder()
    .chatClientBuilder(builder)
    .targetLanguage("en")
    .build();

Query translated = transformer.transform(new Query("什么是 Spring AI?"));
// → Query[text="What is Spring AI?"]
```

**跨语言检索提升 30-50%**。前提:docs 是英文(或某种语言)。

## 为什么需要 Multi-Query?

```
用户: "我工作满 6 年,能休几天假?"

基础 RAG:
  query: "我工作满 6 年,能休几天假?"
  retrieve: 命中"满 5 年 15 天" ✓
  答案: "满 5 年 15 天,满 10 年 20 天"(只命中一条,可能漏)

Multi-Query RAG:
  query 1: "我工作满 6 年,能休几天假?"
  query 2: "工龄 6 年的年假政策"
  query 3: "公司年假是怎么算的?"
  retrieve 3 轮,合并去重:
    - "满 5 年 15 天" ✓
    - "满 10 年 20 天" ✓
    - "年假总数 10-20 天" ✓
  → 更全,答案更准确
```

## 4 种 QueryTransformer 对比

| Transformer | 作用 | 适用 |
|---|---|---|
| `RewriteQueryTransformer` | 改写 query(更清楚) | 模糊 query |
| `TranslationQueryTransformer` | 翻译 query | 跨语言 |
| `CompressionQueryTransformer` | 压缩长 query | 长对话 |
| 自定义 | 任意 | 特殊场景 |

## 完整 RAG Pipeline 流程图

```
用户: "我工作满 6 年,能休几天假?"
  ↓
[QueryTransformer]   ← 可选:改写 / 翻译 / 压缩
  ↓
[QueryExpander]      ← 扩 N 个 query
  ↓
[DocumentRetriever]  ← 每 query 取 top-K
  ↓
[DocumentPostProcessor]  ← 可选:re-rank / 过滤
  ↓
[DocumentJoiner]     ← 合并多路 + 去重
  ↓
[QueryAugmenter]     ← 把 docs 拼进 prompt
  ↓
LLM call
  ↓
答案
```

## 实战模式速查

| 场景 | 配置 |
|---|---|
| 简单 QA | 基础 RAG(1 query 检索) |
| 复杂问题 | Multi-Query(扩 3-5 个)+ Concatenation |
| 跨语言 | TranslationQueryTransformer(中 → 英检索) |
| 长对话 | CompressionQueryTransformer(压缩上下文) |
| 精确度 | Re-ranking(DocumentPostProcessor,见 ch11) |
| 多源融合 | Multiple DocumentRetriever(各取 top-K) |

## 性能权衡

Advanced RAG 提升召回,但也有成本:

| 组件 | 增加延迟 | 增加 token | 提升召回 |
|---|---|---|---|
| Multi-Query (3) | +1-2s(LLM 改写) | +50 tokens/query | +30% |
| Translation | +1s | +30 tokens | +20% (跨语言) |
| Compression | +0.5s | 节省 70% | -5% (轻度损失) |
| Re-ranking | +0.2-1s | 0 | +15-30% (精排) |

**经验值**:
- 简单 FAQ:基础 RAG
- 复杂问题:Multi-Query(必加)+ Re-ranking(预算够)
- 跨语言 docs:Translation + Multi-Query
- 长对话:Compression(必加,避免爆 context)

**总延迟** = retriever(~50ms) + rerank(~200ms) + LLM(~2s) ≈ 2-3s,可接受。

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test
    void contextLoads() {
        // 0 网络(advisor 不会主动调 LLM)
    }
}
```

**要真测 Multi-Query 效果**:在 main 跑 + mock ChatClient 验证。

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| `MultiQueryExpander` 扩太多(>10) | token 烧钱,检索变慢 | 控制在 3-5 个 |
| Query 改写后变样 | 检索跑偏 | 改写时给 system prompt 强调"保留原意" |
| 跨语言翻译丢术语 | "Spring AI" 翻成 "Spring 人工智能" | 用 `targetLanguage` + 排除词 |
| 多个 retriever 重复 | doc 重复 | 用 `DocumentJoiner` 去重 |
| `ContextualQueryAugmenter` 没 context | 答案没基于 doc | 检查 aug 是不是默认空 |
| 组合 advisor 但 LLM 答非所问 | advisor 没生效 | 确认 `defaultAdvisors` 没覆盖 |
| 缓存了 prompt 但内容变 | 缓存命中返回旧答案 | 缓存 key 包含 docId(下一章讲) |
| `topK=20` 太大 | 上下文爆 | topK=3-5,precision > recall |

## 实战部署清单

- [ ] 基础 RAG 跑通(基线)
- [ ] 加 Multi-Query(扩 3 个)
- [ ] 对比基线 vs Multi-Query 召回率
- [ ] 跨语言:加 TranslationQueryTransformer
- [ ] 长对话:加 CompressionQueryTransformer
- [ ] 用 `RetrievalAugmentationAdvisor` 组合全套
- [ ] topK=3-5,similarityThreshold=0.5
- [ ] `mvn test` 0 网络 PASS
- [ ] 监控:看 advisor 触发次数(`/actuator/metrics`)

## 完整代码

[02-rag/10-advanced-rag/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/10-advanced-rag)

## 下一步

- [第 11 章 · Re-ranking →](11-reranking.md)— DocumentPostProcessor 提升 top-K 准确率
- [第 12 章 · RAG Production →](12-rag-production.md)— 增量更新 / 缓存 / 监控
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
