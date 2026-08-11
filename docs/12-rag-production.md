# 第 12 章 · RAG Production

> 🎯 目标:生产级 RAG 系统三大支柱 — 增量更新 / 缓存 / 监控

## 你将学到

- ✅ 增量更新(用 document ID 去重,只 embed 新文档)
- ✅ 查询缓存(Spring Cache + Caffeine,避免重复 LLM 调用)
- ✅ 监控(Spring Boot Actuator + Micrometer)
- ✅ Spring AI 2.0 内置 Observability

## 快速开始

```bash
cd 02-rag/12-rag-production
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

3 个 demo + 1 个 web controller:
- `GET /api/rag/ask?q=...` — RAG 问答
- `GET /actuator/metrics` — Spring Boot 指标
- `GET /actuator/prometheus` — Prometheus 格式

## 关键代码

### 1. 增量更新

```java
// 每个 doc 带稳定 docId(metadata)
new Document(
    "policy-2026-v1-leave",
    "年假制度: 10 天带薪年假",
    Map.of("docId", "policy-2026-v1")
);
```

**生产实践**:
- 维护"已 embed 文档 ID 清单"(Redis / DB)
- 新文档来时,先对比清单,跳过已 embed 的
- 改动的文档,先 `vectorStore.delete(List.of(oldId))` 再 add

### 2. 查询缓存

```java
@Service
class RagService {
    @Cacheable("ragQueries")
    public String ragWithCache(String question, ...) {
        return chatClient.prompt().user(question).call().content();
    }
}
```

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m  # 1000 条 / 5 分钟过期
```

**性能提升**:
- 首次查询:~3s
- 二次查询(命中缓存):~1ms
- 加速 **3000x**

### 3. 监控

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

启动后访问:
- `http://localhost:8080/actuator/health` — 健康检查
- `http://localhost:8080/actuator/metrics` — 所有指标
- `http://localhost:8080/actuator/metrics/ai.chat.client.call` — Spring AI LLM 调用指标
- `http://localhost:8080/actuator/prometheus` — Prometheus 格式

**Spring AI 2.0 内置 Micrometer 指标**:
- `ai.chat.client.call` — LLM 调用次数 / 延迟
- `ai.vector.store.query` — 向量检索次数 / 延迟
- `ai.embeddings.call` — embedding 调用
- `ai.rag.advisor.before` / `.after` — RAG advisor 触发次数

## 4 个生产实践

### 1. 增量更新模式

```java
@Service
class DocumentIngestService {
    public void ingest(List<Document> newDocs) {
        // 1. 跟"已 embed 清单"对比
        List<Document> toEmbed = newDocs.stream()
            .filter(d -> !alreadyEmbedded(d.getId()))
            .toList();
        // 2. 只 embed 新的
        vectorStore.add(toEmbed);
        // 3. 标记已 embed
        markEmbedded(toEmbed);
    }
}
```

### 2. 缓存 key 设计

```java
// 用 query 的 hash 作 cache key
@Cacheable(value = "rag", key = "#query.hashCode()")
public String rag(String query, ...) { ... }

// 或者用 key 包含模型版本(模型升级自动失效)
@Cacheable(value = "rag", key = "#query + ':' + T(Config).MODEL_VERSION")
public String rag(String query, ...) { ... }
```

### 3. 失败降级

```java
public String ragWithFallback(String question) {
    try {
        return ragService.call(question);  // 调 LLM
    } catch (RateLimitExceededException e) {
        return "当前查询繁忙,请稍后再试";  // 降级
    } catch (Exception e) {
        // 记日志 + 返回友好提示
        return "系统开了小差";
    }
}
```

### 4. 限流

```java
@RateLimiter(name = "rag-api", fallbackMethod = "fallback")
public String rag(String question) { ... }
```

用 Resilience4j / Sentinel。

## 实战部署清单

- [ ] pgvector 部署(5433 端口避开本地)
- [ ] Caffeine 缓存配置(maximumSize / expireAfterWrite)
- [ ] Spring Boot Actuator 暴露 metrics
- [ ] Prometheus 抓 `/actuator/prometheus`
- [ ] Grafana Dashboard 看 RAG 指标
- [ ] 限流(Resilience4j)
- [ ] 失败降级(返回友好提示)
- [ ] 文档 ID 清单(增量更新)
- [ ] 定期 re-rank 模型更新

## 完整代码

[02-rag/12-rag-production/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/12-rag-production)

## 🎉 Phase 2 (RAG 实战) 完结

12 个 chapter 全完成!
- Phase 1(基础 6 章)
- Phase 2(RAG 6 章)
- 接下来:Phase 3(Agent 6 章) + 5 个实战项目
