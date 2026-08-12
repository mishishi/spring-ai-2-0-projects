# 真实 LLM 接入指南


## 你将学到

- ✅ 把 23 个模块从 mock 切到真实 LLM,**只改 env vars**
- ✅ 4 种主流 provider 切换方式(OpenAI / 通义千问 / DeepSeek / Ollama)
- ✅ 5 个项目(P1-P5)各自的接入小步骤
- ✅ 成本 / 限流 / 安全 三件套的实战策略

## 为什么默认是 mock

v2 的 23 个模块**默认走 mock 路径**,为了两个目标:

1. **CI / 测试 0 网络** — `mvn test` 跑 74+ 个测试**完全不调真实 LLM**,快速、本地、无费用
2. **零成本试读** — clone 仓库到本地,clone 完立刻能跑,不需要申请 API key

**怎么做到**:每个 `application.yml` 都有 3 个占位符:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:fake-key-for-tests}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o-mini}
```

- **没设环境变量** → 用默认值,Spring context 启动时 ChatClient 是 mock 状态
- **设了环境变量** → 切到真实 LLM

**这意味着**:从 mock 切到真 LLM,**只改 env vars,不碰代码**。

## 4 种 Provider 切换

### 1. OpenAI(默认)

申请: https://platform.openai.com/api-keys

```bash
export OPENAI_API_KEY=sk-proj-xxxxxx
export OPENAI_MODEL=gpt-4o-mini   # 可选,默认 gpt-4o-mini
cd 01-basics/01-hello-world
mvn spring-boot:run
```

**模型选型**:
- `gpt-4o-mini` — 便宜 ($0.15/1M input),大多数场景够用
- `gpt-4o` — 贵 30 倍,质量好一截,复杂推理用
- `gpt-4-turbo` — 平衡选项

### 2. 通义千问(国内推荐)

申请: https://dashscope.console.aliyun.com/

```bash
export OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxx   # 实际是 dashscope key
export OPENAI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
export OPENAI_MODEL=qwen-plus
mvn spring-boot:run
```

**注意**:通义千问提供 **OpenAI 兼容 API**,所以**完全复用** OpenAI starter,只改 `base-url` 和 `model`。

**模型选型**:
- `qwen-turbo` — 最便宜,日常对话
- `qwen-plus` — 平衡(推荐)
- `qwen-max` — 最强,复杂任务

### 3. DeepSeek(国内 + 性价比)

申请: https://platform.deepseek.com/

```bash
export OPENAI_API_KEY=sk-xxxxxxxx
export OPENAI_BASE_URL=https://api.deepseek.com
export OPENAI_MODEL=deepseek-chat
mvn spring-boot:run
```

**DeepSeek 优势**:
- 价格极低(deepseek-chat: $0.14/1M input,接近 gpt-4o-mini 但中文更强)
- **推理模型** `deepseek-reasoner` 跟 o1 同一档,数学/代码/逻辑强
- 完全 OpenAI 兼容

### 4. Ollama(本地 / 零成本)

适合:没 API key、不想花钱、敏感数据不能出网。

#### 4.1 装 Ollama

```bash
# macOS
brew install ollama
# 拉一个模型
ollama pull qwen2.5:7b
ollama serve  # 默认跑 11434 端口
```

#### 4.2 切依赖

Ollama **不**用 OpenAI starter,需要换:

```xml
<!-- pom.xml -->
<dependencies>
    <!-- 删掉这个:
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
    -->

    <!-- 换成: -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>
</dependencies>
```

#### 4.3 切配置

```yaml
# application.yml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:7b    # 或者 llama3 / mistral 等
```

#### 4.4 跑

```bash
mvn spring-boot:run
```

**Ollama 模型选型**:
- `qwen2.5:7b` — 中文最强 7B,推荐起步
- `llama3.1:8b` — 英文强
- `deepseek-coder:6.7b` — 代码专用
- `qwen2.5:14b` — 强一档,要 16G+ 内存

## P1-P5 接入清单

5 个项目结构都跟 Phase 1 一致 — 改 env vars 就完事。

### P1 AI 周报生成器

```bash
cd 04-projects/project-1-weekly-report
export OPENAI_API_KEY=sk-xxx
mvn spring-boot:run
# POST http://localhost:8080/api/weekly-report/generate
```

### P2 企业文档问答

P2 **特殊**:由于 v2 Spring AI 2.0 缺 `spring-ai-vector-store-simple`(见 D-29 决策),P2 走**纯关键字检索**(中文 2-gram),**不调 embedding**。

切到真 LLM:**只影响"答案生成"环节**:

```bash
cd 04-projects/project-2-doc-qa
export OPENAI_API_KEY=sk-xxx
mvn spring-boot:run
# GET http://localhost:8080/api/doc-qa/ask?q=年假
```

检索本身 0 网络,LLM 答案生成会调真实 API。

### P3 AI 旅行规划师

P3 有 **4 个 ChatClient Bean**(景点 / 路线 / 预算 / 报告),都是 OpenAI starter,统一改 env vars:

```bash
cd 04-projects/project-3-travel-planner
export OPENAI_API_KEY=sk-xxx
mvn spring-boot:run
# POST http://localhost:8080/api/travel/plan {"destination":"杭州","days":3}
```

### P4 AI 代码审查器

P4 有 **7 个 @Tool 静态分析**(不调 LLM,纯本地 AST 解析),LLM 只在最后生成"审查报告"时调:

```bash
cd 04-projects/project-4-code-review
export OPENAI_API_KEY=sk-xxx
mvn spring-boot:run
# POST http://localhost:8080/api/code-review/review {"code":"..."}
```

### P5 AI 综合知识中心

P5 整合 RAG + Tool + Memory,LLM 调用最多:

```bash
cd 04-projects/project-5-knowledge-hub
export OPENAI_API_KEY=sk-xxx
mvn spring-boot:run
# POST http://localhost:8080/api/knowledge/ask {"question":"..."}
```

## 风险 & 对策

### 1. API key 泄露

**坑**:`application.yml` 直接写 `sk-xxx` → 提交到 GitHub → 5 分钟内被 OpenAI 封号 + 刷爆额度。

**对策**:

```bash
# ✅ 用环境变量(永远不在 yml 写真实 key)
export OPENAI_API_KEY=sk-xxx

# ✅ .gitignore 加 .env
echo ".env" >> .gitignore
```

Spring 自动从环境变量读 `spring.ai.openai.api-key`,不需要改代码。

### 2. 成本失控

**坑**:跑 demo 忘了关,一晚上烧几十块。

**对策**:

```yaml
# 限制 token(避免失控)
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o-mini          # 永远用 mini 做开发
          max-tokens: 500              # 限输出
          temperature: 0.0             # 确定性高,省 token
```

```java
// Java 侧加防护:每天调用上限
@RateLimiter(name = "llm-api", fallbackMethod = "rateLimitFallback")
public String chat(String prompt) { ... }
```

### 3. 限流(Rate Limit)

OpenAI / 通义免费档都有 QPM 限制(QPM = Queries Per Minute):

| 平台 | 免费档 QPM | 付费档 QPM |
|---|---|---|
| OpenAI | 3 RPM(免费 3 个月) | 500-10000 RPM |
| 通义千问 | 60 QPM | 1000+ QPM |
| DeepSeek | 60 RPM | 3000+ RPM |
| Ollama | 不限(本地) | 不限 |

**对策**:用 Resilience4j 加限流 + 重试:

```java
@Retry(name = "llm-api", fallbackMethod = "retryFallback")
@RateLimiter(name = "llm-api", fallbackMethod = "rateLimitFallback")
public String chat(String prompt) { ... }
```

## 验证清单

切到真 LLM 后跑一下,确认链路通:

- [ ] `mvn test` — 仍然 0 网络 PASS(说明 mock 路径没坏)
- [ ] `mvn spring-boot:run` — 启动成功,看到 `Started Application in X seconds`
- [ ] 调一次 LLM API — 返回真实内容(不是 mock fallback)
- [ ] 故意改错 API key — 看到 401 / 403 错误(说明真在调)
- [ ] 看 Actuator `/actuator/metrics/ai.chat.client.call` — 计数 +1

## 完整代码

- [Phase 1 Hello World](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/01-basics/01-hello-world) — 最简单的接入
- [P1 AI 周报生成器](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/04-projects/project-1-weekly-report) — 端到端示例
- [P5 AI 综合知识中心](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/04-projects/project-5-knowledge-hub) — RAG + Tool + Memory 整合

## 下一步

- 跑通 [第 1 章 · Hello World →](../01-hello-world.md) 切真 LLM
- 进阶看 [第 18 章 · Agent Production →](../18-agent-production.md)(限流 / 缓存 / 降级)
- 想换 RAG 的 embedding 模型?看 [第 10 章 · Advanced RAG →](../10-advanced-rag.md)
