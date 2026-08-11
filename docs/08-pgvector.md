# 第 8 章 · pgvector


## 一句话总结

`PgVectorStore.builder(jdbcTemplate, embeddingModel).withDimensions(1536).withDistanceType(COSINE).withIndexType(HNSW).build()` 替换 SimpleVectorStore,数据落 PostgreSQL,可水平扩展。

## 读者学完能做什么

- 起 Docker pgvector 容器
- 配 PgVectorStore
- 选 dimension / distance / index
- 验证 SQL 查相似度
- 理解 SimpleVectorStore vs PgVectorStore

## 4 个关键配置

```java
PgVectorStore.builder(jdbc, embeddingModel)
    .withDimensions(1536)                 // 跟 embedding model 匹配
    .withDistanceType(COSINE_DISTANCE)    // 文本用 cosine
    .withIndexType(HNSW)                  // 生产推荐
    .initializeSchema(true)               // 自动建表
    .build();
```

## 3 种距离函数

| 距离 | 公式 | 何时用 |
|---|---|---|
| COSINE | 1 - cos(θ) | **文本默认** |
| EUCLIDEAN | √(Σ(a-b)²) | 图像 / 数值 |
| NEGATIVE_INNER_PRODUCT | -Σ(a·b) | 已 normalized |

## 3 种索引

| 索引 | 文档数 | 性能 |
|---|---|---|
| NONE | < 1K | 慢但准 |
| IVFFLAT | 1K-100K | 中等 |
| HNSW | **任意**(默认) | 快 |

## 完整代码

[02-rag/08-pgvector/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/08-pgvector)

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| Docker pgvector 没起 | 启动报 connection refused | `docker ps` 看 pgvector 是否在跑 |
| Dimensions 不匹配 | pgvector 报 dimension error | 跟 embedding model 输出维度对齐 |
| pgvector extension 没装 | 创建表失败 | `CREATE EXTENSION vector` |
| 数据没清,重复跑 | retrieve 出现重复文档 | 删表 / 改用 UUID 文档 ID |
| HNSW 参数不当 | 召回率低 | 调 `efConstruction` / `efSearch` |

## 下一步

- [第 9 章 · Document Loaders →](09-document-loaders.md)
- 真实 PDF/Word/Markdown 文档加载
