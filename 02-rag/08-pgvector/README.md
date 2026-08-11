# 第 8 章 · pgvector

> 🎯 目标:把 SimpleVectorStore 换成真实 PostgreSQL pgvector,数据持久化 + 生产级

## 你将学到

- ✅ 怎么起 pgvector Docker container
- ✅ 怎么用 `PgVectorStore`(Spring AI 2.0 真实 vector store)
- ✅ 维度 / 距离函数 / 索引类型的选择
- ✅ DataSource + JdbcTemplate 集成
- ✅ 真实持久化(重启数据不丢)

## 前置条件:Docker pgvector

```bash
docker run -d --name pgvector -p 5433:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=vectordb \
  pgvector/pgvector:pg16
```

> **端口 5433 原因**:macOS 自带 PostgreSQL(通过 Postgres.app / homebrew),会占用 5432 端口,导致 Docker pgvector 无法连。用 5433 避开。

启动后:
```bash
docker exec pgvector psql -U postgres -d vectordb \
  -c "CREATE EXTENSION IF NOT EXISTS vector;"
# extversion: 0.8.6
```

## 快速开始

```bash
cd 02-rag/08-pgvector
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

3 个 demo:
- Step 1: 存 5 个公司制度文档到 pgvector
- Demo 1: 真实 retrieve top-3
- Demo 2: QuestionAnswerAdvisor 自动 RAG
- Demo 3: 多问题,数据持久化验证

## 关键代码

### 1. 配置 DataSource

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vectordb
    username: postgres
    password: postgres
```

### 2. 配 PgVectorStore bean

```java
@Bean
public VectorStore pgVectorStore(DataSource dataSource, EmbeddingModel embeddingModel) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    return PgVectorStore.builder(jdbcTemplate, embeddingModel)
        .withDimensions(1536)                              // OpenAI text-embedding-3-small
        .withDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
        .withIndexType(PgVectorStore.PgIndexType.HNSW)
        .initializeSchema(true)                            // 自动建表
        .build();
}
```

### 3. 用法跟 SimpleVectorStore 一模一样

```java
// 存
vectorStore.add(documents);

// 查
List<Document> topK = vectorStore.similaritySearch(
    SearchRequest.builder().query(q).topK(3).build()
);

// 全自动 RAG
ChatClient client = builder
    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .build();
```

## 关键配置

### Dimensions(向量维度)

必须跟 EmbeddingModel 输出维度一致:

| Embedding Model | Dimensions |
|---|---|
| OpenAI `text-embedding-3-small` | 1536 |
| OpenAI `text-embedding-3-large` | 3072 |
| OpenAI `text-embedding-ada-002` | 1536 |
| BGE `bge-large-zh-v1.5` | 1024 |
| Ollama `nomic-embed-text` | 768 |
| Ollama `mxbai-embed-large` | 1024 |

**改了 embedding 模型?`.withDimensions(XXX)` 也要改!**

### Distance Type(距离函数)

| 类型 | 公式 | 何时用 |
|---|---|---|
| `COSINE_DISTANCE` | 1 - cos(θ) | 文本 embedding(最常用) |
| `EUCLIDEAN_DISTANCE` | √(Σ(a-b)²) | 图像 / 数值 |
| `NEGATIVE_INNER_PRODUCT` | -Σ(a·b) | 已 normalized 向量 |

**默认 cosine**。

### Index Type(索引)

| 类型 | 速度 | 召回率 | 何时用 |
|---|---|---|---|
| `NONE` | 慢 | 100% | < 1K 文档 |
| `IVFFLAT` | 中 | 95% | 1K-100K 文档 |
| `HNSW` | 快 | 99% | **生产推荐** |

**默认 HNSW**,10K+ 文档也能毫秒级检索。

## pgvector SQL 验证

```bash
# 查看文档数
docker exec pgvector psql -U postgres -d vectordb \
  -c "SELECT COUNT(*) FROM vector_store;"

# 查看 schema
docker exec pgvector psql -U postgres -d vectordb \
  -c "\d vector_store"

# 手动查相似度
docker exec pgvector psql -U postgres -d vectordb \
  -c "SELECT id, content FROM vector_store ORDER BY embedding <=> (SELECT embedding FROM vector_store WHERE id = 1) LIMIT 3;"
```

`<=>` 是 cosine distance 运算符。

## 数据持久化验证

1. 跑一次 `mvn spring-boot:run`,5 文档入 pgvector
2. 退出应用
3. 再跑一次 `mvn spring-boot:run`
4. 这次 retrieve 还能查到(数据持久化!)

vs SimpleVectorStore(内存):**重启就丢**。

## SimpleVectorStore vs PgVectorStore

| 维度 | SimpleVectorStore | PgVectorStore |
|---|---|---|
| 持久化 | ❌ 重启丢 | ✅ 数据库 |
| 容量 | MB 级 | GB / TB |
| 多实例 | ❌ | ✅ 共享 |
| 启动 | 0 配置 | 需 Docker / DB |
| 性能 | 快(内存) | 慢但稳(磁盘 + 索引) |
| 生产 | ❌ | ✅ |
| 教学 | ✅ 推荐 | ✅ 也行 |

## 测试

```bash
mvn test
```

集成测试,连真实 pgvector(localhost:5432)。**VectorStore 延迟初始化,contextLoads 不调 OpenAI,0 网络。**

## 目录结构

```
08-pgvector/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/pgvector/
    │   │   └── Application.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/cc/misshi/springai/pgvector/
            └── ApplicationTests.java
```

## 下一章

[第 9 章 · Document Loaders →](../09-document-loaders/README.md)

加载真实 PDF / Word / Markdown 文档,自动 chunking,塞进 vector store
