# 第 8 章 · pgvector 实战


## 你将学到

- ✅ 起 Docker pgvector 容器(避开 macOS 本地 PG 5432 端口冲突)
- ✅ `PgVectorStore.builder()` 4 个关键配置
- ✅ 3 种距离函数(COSINE / EUCLIDEAN / NEGATIVE_INNER_PRODUCT)选型
- ✅ 3 种索引类型(NONE / IVFFLAT / HNSW)选型
- ✅ QuestionAnswerAdvisor 端到端 RAG
- ✅ 跨进程验证持久化(数据真在数据库,重启还在)

## 一句话总结

`PgVectorStore.builder(jdbcTemplate, embeddingModel).dimensions(1536).distanceType(COSINE_DISTANCE).indexType(HNSW).build()` 替换 SimpleVectorStore,数据落 PostgreSQL,可水平扩展。

## 读者学完能做什么

- 起 Docker pgvector 容器
- 配 PgVectorStore
- 选 dimension / distance / index
- 验证 SQL 查相似度
- 理解 SimpleVectorStore vs PgVectorStore 选型
- 把 RAG 跑在真实数据库上,生产可用

## 5 分钟上手

### 1. 起 Docker pgvector

```bash
# pgvector 镜像(含 vector extension)
docker run -d --name pgvector -p 5433:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=vectordb \
  pgvector/pgvector:pg16
```

**端口用 5433 不用 5432** — 避开 macOS 自带 PostgreSQL 服务(详见 D-18 决策)。

### 2. 验证

```bash
docker ps | grep pgvector
# 应该看到 pgvector/pgvector:pg16 Up

docker exec pgvector psql -U postgres -d vectordb -c "SELECT extname FROM pg_extension WHERE extname='vector';"
# 应该返回: extname = vector
```

### 3. 跑

```bash
export OPENAI_API_KEY=sk-xxxxx
cd 02-rag/08-pgvector
mvn spring-boot:run
```

应用启动时 `initializeSchema(true)` 自动建 `vector_store` 表 + HNSW 索引。

## 为什么需要 pgvector(背景)

ch7 用的 `SimpleVectorStore` 是**纯内存**,两个生产痛点:

1. **重启数据丢** — Spring Boot 重启 → in-memory map 清空 → 必须重新 add 文档
2. **不能水平扩展** — 多实例部署,每个实例都有自己一份数据,不一致

**pgvector 解决了什么**:

| 维度 | SimpleVectorStore | PgVectorStore |
|---|---|---|
| 存储位置 | JVM 内存 | PostgreSQL 磁盘 |
| 重启数据 | 丢失 | 持久化 |
| 容量上限 | 跟 JVM heap 走(GB 级) | TB 级 |
| 检索速度 | O(N) 全表扫 | HNSW 索引 O(log N) |
| 水平扩展 | 不支持 | 复制 / 分片 |
| 适用场景 | Demo / 测试 | 生产 |

**类比**:

```
SimpleVectorStore  ≈  HashMap(进程内)
PgVectorStore      ≈  Redis(独立进程,持久化)
```

## 关键概念(4 个)

### 概念 1:`VectorStore` 接口

Spring AI 抽象的统一接口,**所有** vector store 都实现它(In-memory / pgvector / Milvus / Pinecone):

```java
public interface VectorStore {
    void add(List<Document> documents);
    List<Document> similaritySearch(SearchRequest request);
    Optional<Boolean> delete(List<String> idList);    // 增量更新用
}
```

**好处**:业务代码用 `VectorStore`,底层换 store 不动业务。

### 概念 2:`PgVectorStore` 配置

```java
@Bean
public VectorStore pgVectorStore(DataSource ds, EmbeddingModel embeddingModel) {
    JdbcTemplate jdbc = new JdbcTemplate(ds);
    return PgVectorStore.builder(jdbc, embeddingModel)
        .dimensions(1536)                              // ← 跟 embedding 对齐
        .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
        .indexType(PgVectorStore.PgIndexType.HNSW)
        .initializeSchema(true)                        // 自动建表
        .build();
}
```

**`dimensions(1536)` 必须跟 embedding model 输出维度对齐**:
- OpenAI `text-embedding-3-small` → 1536
- OpenAI `text-embedding-3-large` → 3072
- Ollama `nomic-embed-text` → 768
- 不对齐 → pgvector 报 dimension error

### 概念 3:距离函数

| 距离 | 公式 | 何时用 |
|---|---|---|
| COSINE | 1 - cos(θ) | **文本默认**(归一化无关) |
| EUCLIDEAN | √(Σ(a-b)²) | 图像 / 数值(原始距离有意义) |
| NEGATIVE_INNER_PRODUCT | -Σ(a·b) | 已 normalized 向量 |

**选型**:
- 文本 / embedding:几乎都用 **COSINE**
- 推荐系统(用户向量):EUCLIDEAN
- 已经 L2-normalize 过的向量:NEGATIVE_INNER_PRODUCT(快)

### 概念 4:索引类型

| 索引 | 文档数 | 性能 | 召回率 |
|---|---|---|---|
| NONE | < 1K | 慢但准(顺序扫) | 100% |
| IVFFLAT | 1K-100K | 中等 | 95-98% |
| HNSW | **任意**(默认) | 快 | 99%+ |

**HNSW 调优**:

```java
PgVectorStore.builder(...)
    .indexType(HNSW)
    // Spring AI 2.0 默认 efConstruction=16, efSearch=64
    // 大数据集可以调高 efSearch 提高召回
    .build();
```

## 关键代码(4 个实战场景)

### 场景 1:基础配置(推荐起步)

```java
PgVectorStore.builder(jdbcTemplate, embeddingModel)
    .dimensions(1536)
    .distanceType(COSINE_DISTANCE)
    .indexType(HNSW)
    .initializeSchema(true)
    .build();
```

### 场景 2:换距离函数(图像 embedding)

```java
PgVectorStore.builder(jdbcTemplate, clipEmbeddingModel)   // CLIP 图像向量
    .dimensions(512)                                       // CLIP 输出 512 维
    .distanceType(PgVectorStore.PgDistanceType.EUCLIDEAN_DISTANCE)
    .indexType(PgVectorStore.PgIndexType.HNSW)
    .build();
```

### 场景 3:QuestionAnswerAdvisor 全自动 RAG

```java
ChatClient client = builder
    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .defaultSystem("你是 HR 助手,基于公司制度回答")
    .build();

String answer = client.prompt()
    .user("我年假有几天?")
    .call()
    .content();
```

`QuestionAnswerAdvisor` 内部:
1. retrieve top-K 文档
2. 把文档塞进 user message 的 context
3. 调 LLM 生成答案
4. **不用你手动拼 prompt**

### 场景 4:手动 retrieve + 自定义

```java
List<Document> top3 = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query("年假")
        .topK(3)
        .similarityThreshold(0.7)        // 相似度阈值
        .filterExpression("type == 'policy'")  // 元数据过滤
        .build()
);

top3.forEach(d -> log.info("📄 {}", d.getText()));
```

**`similarityThreshold(0.7)`**:低于 0.7 不返回,避免 LLM 拿到不相关文档乱答。
**`filterExpression`**:基于 metadata 过滤,比如只看 `type=policy` 的文档。

## 跨进程验证持久化

```bash
# 第一次跑
mvn spring-boot:run
# 存了 5 个文档

# 第二次跑(直接重启)
mvn spring-boot:run
# retrieve 还能拿到 5 个文档 — 数据落 PostgreSQL,没丢
```

对比 ch7 SimpleVectorStore:**第二次跑 retrieve 是空的**(因为新进程,内存 map 重建)。

## 清空数据

```bash
# 方式 1:TRUNCATE(快,清表)
docker exec pgvector psql -U postgres -d vectordb -c "TRUNCATE vector_store;"

# 方式 2:DROP(连表结构一起删)
docker exec pgvector psql -U postgres -d vectordb -c "DROP TABLE vector_store;"
```

## 测试(纯本地 0 网络)

ch8 测试需要 Docker pgvector 在跑,所以**不是完全 0 网络**。但可以用 H2 内存数据库 mock:

```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test
    void contextLoads() {
        // VectorStore 是 lazy init,@SpringBootTest 不主动调
        // 启动时不会连接 pgvector
    }
}
```

Spring AI 2.0 的 `VectorStore` bean 是 `lazy` 的 — `@SpringBootTest` 默认不触发 `vectorStore.add()`,所以**不会连接数据库**。

**要真测 pgvector**:在 CI 起 Docker pgvector 容器(用 testcontainers),见 ch12 RAG Production 实战。

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| Docker pgvector 没起 | 启动报 connection refused | `docker ps` 看 pgvector 是否在跑 |
| Dimensions 不匹配 | pgvector 报 dimension error | 跟 embedding model 输出维度对齐 |
| pgvector extension 没装 | 创建表失败 | 镜像必须 `pgvector/pgvector:pg16`,不能 `postgres:16` |
| 数据没清,重复跑 | retrieve 出现重复文档 | TRUNCATE vector_store 或用 docId 去重 |
| HNSW 参数不当 | 召回率低 | 调 `efSearch`(默认 64,可调到 100-200) |
| 端口 5432 冲突 | macOS 自带 PG 占 5432 | 用 5433 端口 |
| `vectorStore.add()` 超时 | 网络慢 / doc 太多 | 分批 add,每批 100 个 |
| metadata filter 不生效 | `filterExpression` 语法错 | 字段名跟 document metadata key 一致 |

## 实战部署清单

- [ ] Docker 拉 `pgvector/pgvector:pg16` 镜像
- [ ] 容器跑 5433 端口(避开 5432)
- [ ] 验证 `vector` extension 装好
- [ ] application.yml 配 `spring.datasource.url=jdbc:postgresql://localhost:5433/vectordb`
- [ ] `dimensions(1536)` 跟 `text-embedding-3-small` 对齐
- [ ] 选 HNSW 索引(生产)
- [ ] `initializeSchema(true)` 自动建表
- [ ] 跑 `mvn spring-boot:run` 看到 "started"
- [ ] retrieve 拿到正确文档
- [ ] 重启验证数据持久化
- [ ] 监控:pgvector 磁盘占用 / 索引大小

## 完整代码

[02-rag/08-pgvector/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/08-pgvector)

## 下一步

- [第 9 章 · Document Loaders →](09-document-loaders.md)— 真实 PDF/Word/Markdown 文档加载
- [第 12 章 · RAG Production →](12-rag-production.md)— 增量更新 / 缓存 / 监控
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
