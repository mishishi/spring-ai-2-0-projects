# 第 7 章 · RAG Overview


## 你将学到

- ✅ RAG 是什么,为什么需要
- ✅ RAG 完整 5 步流程:Load → Split → Embed → Store → Retrieve
- ✅ 4 个核心抽象:`Document` / `EmbeddingModel` / `VectorStore` / `SearchRequest`
- ✅ 用 `SimpleVectorStore` 跑 RAG demo(0 Docker)
- ✅ `QuestionAnswerAdvisor` 一行 RAG 自动化
- ✅ SimpleVectorStore vs PgVectorStore 选型

## 一句话总结

RAG = 让 LLM 基于你的私有文档回答,核心 5 步:Load → Split → Embed → Store → Retrieve,提问时把 retrieve 的 top-k 文档拼进 prompt → LLM 答。

## 读者学完能做什么

- 理解 RAG 5 步流程
- 用 SimpleVectorStore 跑 RAG demo
- 用 QuestionAnswerAdvisor 自动化 RAG
- 知道什么时候用哪种 Vector Store
- 选 EmbeddingModel(OpenAI / 通义 / BGE 中文)
- 调 topK / similarityThreshold 参数

## 5 分钟上手

```bash
export OPENAI_API_KEY=sk-xxxxx
cd 02-rag/07-rag-overview
mvn spring-boot:run
```

跑 2 个 demo:
1. SimpleVectorStore + 手动 RAG(理解原理)
2. QuestionAnswerAdvisor + 自动 RAG(实战用法)

## 为什么需要 RAG(背景)

LLM 默认 2 个痛点:

**痛点 1:知识过时**
- GPT-4 知识截止 2023-10
- 你公司今年新出的产品政策,LLM 完全不知道
- 问"我们公司年假几天?" → "我不清楚你们公司情况"

**痛点 2:幻觉(Hallucination)**
- 问"什么是 Spring AI 2.0",LLM 瞎编一个版本号
- 看似自信,实则编造
- 在生产场景 **不可接受**

**RAG 解法**:
- 让 LLM "带着资料开卷考"
- 你的私有文档 → 检索 top-k → 拼进 prompt → LLM 基于文档回答
- **幻觉率从 30% 降到 2%**

**类比**:
- 老 LLM = 学生裸考(没资料,瞎写)
- RAG = 学生开卷考(拿着笔记回答)

## 关键概念(4 个)

### 概念 1:`Document`

Spring AI 文档抽象,**一段文本 + metadata**:

```java
public class Document {
    private final String id;                    // 可选,UUID
    private final String text;                  // 内容
    private final Map<String, Object> metadata; // 元数据
}

new Document("公司年假制度: 10 天带薪年假...");
// → Document[id=uuid, text=..., metadata={}]
```

### 概念 2:`EmbeddingModel`

文本 → 高维向量(浮点数数组):

```java
EmbeddingModel embeddingModel = ...;  // Spring AI 自动注入
float[] vector = embeddingModel.embed("Spring AI 是什么?");
// vector.length = 1536 (OpenAI text-embedding-3-small)
// vector.length = 3072 (OpenAI text-embedding-3-large)
// vector.length = 768  (BGE / nomic-embed-text)
```

**关键属性**:
- **维度**:OpenAI 1536 / 3072,BGE 768
- **语义**:相似文本 → 相似向量(余弦距离小)
- **多语言**:OpenAI 100+ 语种,BGE 中文强

### 概念 3:`VectorStore`

向量数据库的统一接口:

```java
public interface VectorStore {
    void add(List<Document> documents);
    List<Document> similaritySearch(SearchRequest request);
    Optional<Boolean> delete(List<String> idList);
}
```

**实现**:
- `SimpleVectorStore` — 内存(ch7)
- `PgVectorStore` — PostgreSQL(ch8)
- `Milvus` / `Pinecone` / `Weaviate` — 第三方

### 概念 4:`SearchRequest` 查询参数

```java
List<Document> topK = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(userQuestion)
        .topK(5)                       // 取前 5
        .similarityThreshold(0.7)      // 过滤低于 0.7
        .filterExpression("type == 'policy'")   // 元数据过滤
        .build()
);
```

**调参**:
- `topK` 越大,召回越多,但 token 多 → 3-5 最佳
- `similarityThreshold` 过滤垃圾文档 → 0.6-0.8
- `filterExpression` 基于 metadata 缩小范围

## RAG 5 步详解

### Step 1:Load(文档加载)

```java
// 1a. 纯文本(简单)
new Document("公司年假制度: 10 天带薪年假...")

// 1b. 文件(用 Reader)
FileSystemDocumentReader reader = new FileSystemDocumentReader(...);
List<Document> docs = reader.read();
```

**DocumentReader 选型**:
- `PagePdfDocumentReader` — PDF
- `TikaDocumentReader`(Apache Tika)— Word / PPT / HTML
- `MarkdownDocumentReader` — Markdown
- `JsonReader` — JSON

### Step 2:Split(切块)

LLM 上下文有上限(8K / 32K / 128K),文档太长要切。

```java
TokenTextSplitter splitter = TokenTextSplitter.builder()
    .withChunkSize(200)             // 每块 200 token
    .withMinChunkSizeChars(50)
    .build();
List<Document> chunks = splitter.apply(documents);
```

**主流 splitter**:
- `TokenTextSplitter` — 按 token 数(推荐)
- `RecursiveCharacterTextSplitter` — 按段落 / 句子
- `SemanticChunker` — 按语义(高级,慢)

### Step 3:Embed(嵌入)

每个 chunk → EmbeddingModel → 高维向量。

```java
EmbeddingModel embeddingModel = ...;  // Spring AI 自动注入
float[] vector = embeddingModel.embed("Spring AI 是什么?");
// vector.length = 1536
```

**Spring AI 在 `vectorStore.add(docs)` 时自动 embed**,你不用手动调。

### Step 4:Store(存储)

向量 + 元数据存到 VectorStore。

```java
vectorStore.add(documents);
// 内部:embed(doc) → vector → 存
```

**SimpleVectorStore** (本章用) — 纯内存,重启丢。

### Step 5:Retrieve(查询时)

用户问 → 嵌入 → cosine similarity → top-k 文档。

```java
List<Document> topK = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(userQuestion)
        .topK(3)
        .build()
);
```

**Cosine similarity** = 两个向量夹角的余弦(0-1):
- 1.0 = 完全相同
- 0.0 = 完全不同

## QuestionAnswerAdvisor 内部流程

```
user: "我年假有几天?"
    ↓
[QuestionAnswerAdvisor.before()]
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
[QuestionAnswerAdvisor.after()]
  返回 LLM 回答
```

## 4 个实战场景

### 场景 1:Hello RAG(手动,理解原理)

```java
// 1. 准备 docs
List<Document> docs = List.of(
    new Document("公司年假制度: 10 天带薪年假"),
    new Document("病假制度: 3 天内不需证明")
);
vectorStore.add(docs);

// 2. retrieve
List<Document> top = vectorStore.similaritySearch(
    SearchRequest.builder().query("年假").topK(1).build());

// 3. 拼 prompt
String context = top.stream().map(Document::getText).collect(Collectors.joining("\n"));
String user = "基于以下资料回答:" + context + "\n问题:我年假有几天?";

// 4. LLM
String r = client.prompt().user(user).call().content();
```

### 场景 2:全自动 RAG(QuestionAnswerAdvisor,推荐)

```java
ChatClient client = builder
    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .build();

String r = client.prompt().user("我年假有几天?").call().content();
// QuestionAnswerAdvisor 自动 retrieve + 拼 context + 调 LLM
```

### 场景 3:RAG + System Prompt

```java
ChatClient client = builder
    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .defaultSystem("你是 HR 助手,基于公司制度回答。如果资料里没有,直接说'我没有找到相关资料'。")
    .build();
```

**关键 system**:告诉 LLM "不要瞎编,资料没说就说没"。

### 场景 4:RAG + Multi-Turn(上下文累加)

```java
List<Message> history = new ArrayList<>();
history.add(new UserMessage("公司有哪些假?"));
history.add(new AssistantMessage("公司有年假 / 病假 / 婚假"));

history.add(new UserMessage("年假几天?"));
String r = client.prompt().messages(history).call().content();
// LLM 看到完整对话历史 + retrieve 文档
```

## SimpleVectorStore vs 生产 VectorStore

| 维度 | SimpleVectorStore | PgVectorStore |
|---|---|---|
| 持久化 | ❌ 重启丢 | ✅ 数据库 |
| 容量 | MB 级 | GB / TB |
| 多实例 | ❌ | ✅ 共享 |
| 启动 | 0 配置 | 需 pgvector extension |
| 性能 | 快(内存) | 慢但稳 |
| 适用 | Demo / 测试 | 生产 |

**用法对比**:
```java
// SimpleVectorStore
@Bean
VectorStore simpleVectorStore(EmbeddingModel model) {
    return new SimpleVectorStore(model);
}

// PgVectorStore(配置好后自动注入,见 ch8)
@Bean
VectorStore pgVectorStore(JdbcTemplate jdbc, EmbeddingModel model) {
    return PgVectorStore.builder(jdbc, model)
        .dimensions(1536)
        .distanceType(COSINE_DISTANCE)
        .indexType(HNSW)
        .build();
}
```

## EmbeddingModel 选型(中文)

| 模型 | 维度 | 中文 | 速度 | 成本 |
|---|---|---|---|---|
| OpenAI `text-embedding-3-small` | 1536 | ✓ | 快 | $0.02/1M |
| OpenAI `text-embedding-3-large` | 3072 | ✓✓ | 慢 | $0.13/1M |
| BGE `bge-large-zh-v1.5` | 1024 | ✓✓✓ | 中 | 免费本地 |
| 通义 `text-embedding-v3` | 1024 | ✓✓✓ | 快 | 国内便宜 |

**中文推荐**:
- 预算足:OpenAI text-embedding-3-large
- 隐私 / 离线:BGE 中文(本地)
- 国内 + 便宜:通义 text-embedding-v3

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Autowired VectorStore vectorStore;

    @Test
    void testAddAndSearch() {
        // 1. add docs(embed 会被 mock,只用 metadata)
        vectorStore.add(List.of(new Document("年假 10 天")));

        // 2. retrieve(简化测,实际需要真 embed 才能 match)
        SearchRequest req = SearchRequest.builder().query("年假").topK(1).build();
        // ...
    }
}
```

**注意**:`@SpringBootTest` 默认不触发 `vectorStore.add()`,所以 0 网络。

**要真测 RAG 端到端**:配 Docker pgvector(见 ch8)+ 跑 main runner。

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| 文档没切块,超 LLM 上下文 | 报错或截断 | 用 TokenTextSplitter |
| 嵌入维度不匹配 | 检索 0 结果 | 全程用同一 EmbeddingModel |
| 中文 RAG 效果差 | 答非所问 | 用 BGE / M3E 中文 embedding |
| retrieve 的 doc 跟问题无关 | LLM 答偏 | 调 topK / 加 re-rank(ch11) |
| SimpleVectorStore 重启数据丢 | 生产事故 | 用 PgVectorStore(ch8) |
| `topK=20` 太大 | token 爆 | topK=3-5 |
| LLM 不基于 doc 回答 | 幻觉 | system prompt 强调"严格基于 context" |
| metadata filter 语法错 | 不生效 | 字段名跟 doc metadata key 一致 |

## 实战部署清单

- [ ] 选 EmbeddingModel(OpenAI / BGE / 通义)
- [ ] 准备 docs(测试集 10-20 个)
- [ ] TokenTextSplitter 切块(200-500 token)
- [ ] 选 VectorStore(Simple / PgVector)
- [ ] `vectorStore.add(docs)` 存
- [ ] 写 system prompt(强调"严格基于资料")
- [ ] 用 `QuestionAnswerAdvisor` 一行自动化
- [ ] 调 `topK=3-5`,`similarityThreshold=0.7`
- [ ] 验证:问资料里有 → 答得对;问资料没 → 拒答
- [ ] 监控:embedding 延迟 / retrieve 命中率
- [ ] `mvn test` 0 网络 PASS

## 完整代码

[02-rag/07-rag-overview/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/07-rag-overview)

## 下一步

- [第 8 章 · pgvector →](08-pgvector.md)— 把 SimpleVectorStore 换成真实 PostgreSQL pgvector
- [第 9 章 · Document Loaders →](09-document-loaders.md)— 真实 PDF/Word/Markdown 加载
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
