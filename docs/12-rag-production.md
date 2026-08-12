# 第 12 章 · RAG Production 生产实战


## 你将学到

- ✅ 增量更新(用 document ID 去重,只 embed 新文档)
- ✅ 查询缓存(Spring Cache + Caffeine,避免重复 LLM 调用)
- ✅ 监控(Spring Boot Actuator + Micrometer)
- ✅ Spring AI 2.0 内置 Observability
- ✅ 限流降级(Resilience4j)
- ✅ 6 个生产实践:增量 / 缓存 / 降级 / 限流 / 监控 / 文档 ID 管理

## 一句话总结

把 ch7-11 的 demo 级 RAG 升级到生产级:增量更新省成本、查询缓存提速度、Actuator 暴露指标、Resilience4j 限流降级。Spring AI 2.0 内置 Micrometer 指标,**开箱即用**。

## 读者学完能做什么

- 增量更新 docs(避免重复 embed)
- 缓存热门 query(Caffeine 3000x 加速)
- 暴露 RAG 指标到 Prometheus
- 用 Resilience4j 限流
- 失败降级(返回友好提示)
- 部署到生产环境的完整 checklist

## 5 分钟上手

```bash
cd 02-rag/12-rag-production
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

3 个 demo + 1 个 web controller:
- `GET /api/rag/ask?q=...` — RAG 问答
- `GET /actuator/health` — 健康检查
- `GET /actuator/metrics` — 所有指标
- `GET /actuator/prometheus` — Prometheus 格式

## 为什么需要 RAG Production(背景)

ch7-11 的 RAG 跑通 demo 没问题,但生产环境 6 大痛点:

| 痛点 | 现象 | 解决方案 |
|---|---|---|
| **增量更新** | 文档改了,全量重 embed(烧钱) | docId 去重 + 增量 |
| **重复 query** | 同一问题 100 人问,LLM 100 次 | Caffeine 缓存 |
| **没指标** | 不知道 LLM 调用次数 / 延迟 / 失败率 | Actuator + Prometheus |
| **限流** | 用户疯狂刷 API,限流没做被 OpenAI 封 | Resilience4j |
| **失败降级** | LLM 挂了,白屏 | fallback 友好提示 |
| **重启数据丢** | SimpleVectorStore in-memory | pgvector(已 ch8 解决) |

**RAG Production = ch7-11 demo + 上述 6 个生产级补丁**。

## 关键概念(4 个)

### 概念 1:增量更新(避免重复 embed)

**问题**:每次 `vectorStore.add(docs)` 都重新 embed,**100 个文档 = 100 次 LLM 调用**。

**解法**:用稳定 docId 去重:

```java
// 每个 doc 带稳定 docId(metadata)
new Document(
    "policy-2026-v1-leave",                          // ← 稳定 ID
    "年假制度: 10 天带薪年假",
    Map.of("docId", "policy-2026-v1")
);
```

**生产实践**:
- 维护"已 embed 文档 ID 清单"(Redis / DB)
- 新文档来时,先对比清单,跳过已 embed 的
- 改动的文档,先 `vectorStore.delete(List.of(oldId))` 再 add

### 概念 2:查询缓存(避免重复 LLM 调用)

**问题**:同一问题 100 人问 → 100 次 LLM 调用 → 烧钱 + 慢。

**解法**:Spring Cache + Caffeine:

```java
@Service
class RagService {
    @Cacheable("ragQueries")
    public String ragWithCache(String question) {
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

### 概念 3:Spring Boot Actuator + Micrometer

暴露 RAG 指标到 Prometheus / Grafana:

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

### 概念 4:限流 + 降级(Resilience4j)

防止 LLM API 被刷爆 + 失败时友好降级:

```java
@RateLimiter(name = "rag-api", fallbackMethod = "fallback")
public String rag(String question) { ... }

private String fallback(String question, Throwable t) {
    log.warn("RAG fallback, reason: {}", t.getMessage());
    return "当前查询繁忙,请稍后再试";   // 降级
}
```

```yaml
resilience4j:
  ratelimiter:
    instances:
      rag-api:
        limit-for-period: 100        # 100 QPS
        limit-refresh-period: 1s
        timeout-duration: 0          # 立即拒绝(不排队)
```

## 6 个生产实践

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
        if (!toEmbed.isEmpty()) vectorStore.add(toEmbed);
        // 3. 标记已 embed
        markEmbedded(toEmbed);
    }

    private boolean alreadyEmbedded(String docId) {
        return redisTemplate.hasKey("embed:" + docId);
    }

    private void markEmbedded(List<Document> docs) {
        docs.forEach(d ->
            redisTemplate.opsForValue()
                .set("embed:" + d.getId(), "1", Duration.ofDays(30))
        );
    }
}
```

### 2. 缓存 key 设计

```java
// 方案 1:query hash(简单)
@Cacheable(value = "rag", key = "#query.hashCode()")
public String rag(String query) { ... }

// 方案 2:query + 模型版本(模型升级自动失效)
@Cacheable(value = "rag", key = "#query + ':' + T(Config).MODEL_VERSION")
public String rag(String query) { ... }

// 方案 3:query + docId 列表(文档更新自动失效)
@Cacheable(value = "rag", key = "#query + ':' + #docVersion")
public String rag(String query, String docVersion) { ... }
```

### 3. 失败降级(2 层)

```java
public String ragWithFallback(String question) {
    try {
        return ragService.call(question);                          // 1) 调 LLM
    } catch (RateLimitExceededException e) {
        metrics.counter("rag.fallback.rate_limit").increment();
        return "当前查询繁忙,请稍后再试";                          // 2a) 限流降级
    } catch (Exception e) {
        metrics.counter("rag.fallback.error").increment();
        log.error("RAG failed", e);
        return cachedOrDefault(question);                          // 2b) 错误降级
    }
}
```

**降级优先级**:
1. 缓存命中(最快)
2. 默认回复("系统开了小差")
3. 兜底 doc 答案(有内容,只是不准)

### 4. 限流(Resilience4j)

```java
@RateLimiter(name = "rag-api", fallbackMethod = "rateLimitFallback")
public String rag(String question) { ... }

@Retry(name = "rag-api", fallbackMethod = "retryFallback")    // 失败重试 3 次
public String ragWithRetry(String question) { ... }

@Bulkhead(name = "rag-api", type = Bulkhead.Type.SEMAPHORE)   // 信号量隔离
public String ragWithBulkhead(String question) { ... }
```

### 5. 监控(Spring AI 内置)

**Spring AI 2.0 内置 Micrometer 指标**(开箱即用):

| 指标 | 含义 |
|---|---|
| `ai.chat.client.call` | LLM 调用次数 / 延迟 |
| `ai.vector.store.query` | 向量检索次数 / 延迟 |
| `ai.embeddings.call` | embedding 调用 |
| `ai.rag.advisor.before` / `.after` | RAG advisor 触发次数 |

```bash
# 看具体指标
curl http://localhost:8080/actuator/metrics/ai.chat.client.call
# {
#   "name": "ai.chat.client.call",
#   "measurements": [
#     {"statistic": "COUNT", "value": 42.0},
#     {"statistic": "TOTAL_TIME", "value": 127.5}
#   ]
# }
```

### 6. 文档 ID 管理

```java
// 规范:稳定的 docId,跟内容 hash 绑定
String docId = "policy-" + policy.getVersion() + "-" + policy.getId();
// 例: "policy-2026-v1-leave"

Document doc = new Document(docId, text, Map.of(
    "docId", docId,
    "version", "2026-v1",
    "source", "wiki/leave-policy"
));
```

**重 embed 触发条件**:
- docId 变了(新版本)
- content hash 变了(内容改)
- metadata.version 变了

## 完整生产架构图

```
用户请求
  ↓
[Resilience4j]  ← 限流 / 重试 / 隔离
  ↓
[RagService]  ← @Cacheable 缓存
  ↓ (cache miss)
[RetrievalAugmentationAdvisor]  ← ch10-11 组合
  ↓
[VectorStore (pgvector)]
  ↓
[LLM (OpenAI / 通义 / DeepSeek)]
  ↓
[Actuator / Prometheus]  ← 全程指标采集
```

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test
    void contextLoads() {
        // 0 网络(Cache / Resilience4j / Actuator 都不主动调 LLM)
    }
}
```

**要真测**:
- 测 `@Cacheable` 命中:调两次 service,看第二次是否真的命中
- 测 `@RateLimiter` 触发:并发 100 个请求
- 测指标:用 `MeterRegistry` 验证 counter 增减

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| 缓存命中返回旧答案 | doc 更新了,缓存还返回老 doc | 缓存 key 包含 docVersion |
| 限流阈值太低 | 用户 503 报错 | 监控后慢慢调,不要拍脑袋 |
| Resilience4j 没生效 | 限流没拦截 | 确认依赖 + `@EnableAspectJAutoProxy` |
| Actuator 暴露了敏感信息 | security 风险 | `include: health,info,metrics,prometheus`(白名单) |
| 指标名打错 | Prometheus 抓不到 | 用 `ai.chat.client.call`(Spring AI 2.0 内置) |
| 增量更新没删旧 doc | 旧 doc 还在 vector store | `vectorStore.delete(List.of(oldId))` |
| docId 重复 | 同 doc 加了 N 次 | 用 UUID 或 hash 作 docId |
| 缓存击穿(热点 key 失效) | 全部打到 LLM | Caffeine 软引用 + 异步刷新 |

## 实战部署清单

- [ ] pgvector 部署(5433 端口避开本地)
- [ ] 用稳定 docId(`policy-{ver}-{id}`)
- [ ] Redis 维护"已 embed 清单"
- [ ] Caffeine 缓存配置(`maximumSize=1000,expireAfterWrite=5m`)
- [ ] 缓存 key 包含 `docVersion`(自动失效)
- [ ] Spring Boot Actuator 暴露 `health,info,metrics,prometheus`
- [ ] Prometheus 抓 `/actuator/prometheus`
- [ ] Grafana Dashboard 看 RAG 指标
- [ ] Resilience4j 限流(`@RateLimiter`)
- [ ] 失败降级(返回友好提示 + 兜底 doc)
- [ ] `mvn test` 0 网络 PASS

## 完整代码

[02-rag/12-rag-production/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/12-rag-production)

## 🎉 Phase 2 (RAG 实战) 完结

12 个 chapter 全完成!
- Phase 1(基础 6 章)
- Phase 2(RAG 6 章)
- 接下来:Phase 3(Agent 6 章) + 5 个实战项目

## 下一步

- [Phase 3 总览 →](overviews/phase-3.md)— Agent 实战开始
- [第 13 章 · Agent Basics →](13-agent-basics.md)— @Tool + LLM 完整 Agent
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
