# 第 16 章 · Multi-Agent 编排


## 一句话总结

**Orchestrator-Workers 模式(Anthropic 风格)** — 主 Agent 调度 3 个 sub-agent(researcher / writer / reviewer)协作,Spring AI 2.0 用多个独立 ChatClient Bean 隔离角色。

## 你将学到

- ✅ Orchestrator-Workers 模式(Anthropic 2024 提出)
- ✅ 4 个 ChatClient Bean,每个独立 system prompt
- ✅ `OrchestrationService` 编程式编排
- ✅ 0 网络 mock 验证 Bean 装配 + 流程编排
- ✅ 多 agent 协作的真实 LLM 路径

## 快速开始

```bash
cd 03-agent/16-multi-agent
mvn test                          # 0 网络 3 tests
mvn spring-boot:run

curl -X POST http://localhost:8080/multi-agent/run \
  -H 'Content-Type: application/json' \
  -d '{"request": "写一篇关于 Spring AI 2.0 的短文"}'
```

## 关键架构:Orchestrator-Workers

```
                    User Request
                         │
                         ▼
              ┌──────────────────────┐
              │  OrchestratorAgent   │ (主 Agent)
              └──────┬───────┬───────┘
                     │       │       │
              ┌──────▼─┐ ┌───▼────┐ ┌▼────────┐
              │Researcher│ │ Writer │ │ Reviewer│
              │(sub)    │ │(sub)   │ │(sub)    │
              └────────┘ └────────┘ └─────────┘
                     │       │       │
              ┌──────▼───────▼───────▼────────┐
              │  facts + draft + review         │
              │  → Orchestrator 整合 → 输出    │
              └─────────────────────────────────┘
```

**3 个 sub-agent + 1 orchestrator**:
- **Researcher** — 收集信息、列举事实、引用来源
- **Writer** — 把事实组织成流畅中文段落
- **Reviewer** — 检查事实准确性、语气、流畅度
- **Orchestrator** — 调度 3 个 sub-agent,整合结果

## 关键 API

### 1. 4 个独立 ChatClient Bean

```java
@Configuration
public class AgentsConfig {

    @Bean(name = "researcherAgent")
    public ChatClient researcherAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 Researcher(研究员)。
                职责: 收集信息、列举事实、引用来源(可 mock)。
                输出格式: 简洁的事实清单,每条 1 行。
                """).build();
    }

    @Bean(name = "writerAgent")
    public ChatClient writerAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 Writer(写作员)。
                职责: 把事实信息组织成流畅的中文段落。
                风格: 简洁,不超过 200 字。
                """).build();
    }

    @Bean(name = "reviewerAgent")
    public ChatClient reviewerAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 Reviewer(审核员)。
                职责: 检查草稿的事实准确性、语气、流畅度,给出修改建议。
                输出: 3 条以内的 bullet list 反馈。
                """).build();
    }

    @Bean(name = "orchestratorAgent")
    public ChatClient orchestratorAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                你是 Orchestrator(协调员)。
                接到用户请求后,你会:
                1. 调 researcherAgent 收集事实
                2. 调 writerAgent 写初稿
                3. 调 reviewerAgent 审核
                4. 整合最终结果给用户
                """).build();
    }
}
```

**核心**:
- 4 个独立 Bean,各自 system prompt 决定角色
- 真实 LLM 时,Orchestrator 调 sub-agent 用 FunctionToolCallback(略复杂,见 14 章)
- 0 网络时,我们手动在 Service 里串接,验证流程

### 2. 编程式 Orchestration Service

```java
@Service
public class OrchestrationService {
    private final ChatClient researcher;
    private final ChatClient writer;
    private final ChatClient reviewer;
    private final ChatClient orchestrator;

    public OrchestrationService(
            @Qualifier("researcherAgent") ChatClient researcher,
            @Qualifier("writerAgent") ChatClient writer,
            @Qualifier("reviewerAgent") ChatClient reviewer,
            @Qualifier("orchestratorAgent") ChatClient orchestrator) { ... }

    public PipelineResult runPipeline(String userRequest, String mockedResearch) {
        // 1. Research
        // 真实: researcher.prompt(userRequest).call().content();
        String facts = mockedResearch;  // 0 网络 mock

        // 2. Write
        // 真实: writer.prompt("事实清单:\n" + facts).call().content();
        String draft = "[MOCK DRAFT based on " + facts.length() + " chars] ...";

        // 3. Review
        // 真实: reviewer.prompt("审稿:\n" + draft).call().content();
        String review = "[MOCK REVIEW] 事实清晰, 建议增加第 2 段细节";

        // 4. Orchestrator 整合
        String finalAnswer = "【研究报告】\n\n" + draft + "\n\n【审核建议】\n" + review;
        return new PipelineResult(facts, draft, review, finalAnswer);
    }
}
```

**关键设计**:
- 0 网络下,所有 sub-agent 都是 mocked(传 null ChatClient)
- 真实 LLM 下,sub-agent 调 `.prompt(...).call().content()` 自动接管
- `runPipeline(userRequest, mockedResearch)` 接受外部 facts 注入,方便测试

## 3 个 Demo

### Demo 1: 写短文

```bash
curl -X POST http://localhost:8080/multi-agent/run \
  -H 'Content-Type: application/json' \
  -d '{"request": "写一篇关于 Spring AI 2.0 的短文"}'
```

返回 4 段:
- `facts` — 5 条 Spring AI 2.0 事实(mock)
- `draft` — 写作员产出的初稿
- `review` — 审核员反馈
- `finalAnswer` — Orchestrator 整合的最终结果

### Demo 2: 多角度研究

实际场景:把 `mockedResearch` 替换成 Orchestrator 调 Researcher 真实 LLM 的结果,流程不变。

### Demo 3: 自定义 facts

在 controller 里把 `mockedFacts` 替换为传入的事实,适合"已经有数据,直接加工成稿"场景。

## 踩坑(3 大常见)

### 坑 1: `@Qualifier` 名字写错

```java
// ❌ Bean 名字跟 @Qualifier 不一致
@Bean(name = "writerAgent") public ChatClient writerAgent(...)

public OrchestrationService(@Qualifier("WriterAgent") ChatClient writer) { ... }
// → NoSuchBeanDefinitionException

// ✅ 名字严格一致
@Qualifier("writerAgent")
```

### 坑 2: Orchestrator 没真去调 sub-agent

**0 网络 mock 模式** ≠ **真实 LLM 模式**。本章用编程式调用顺序演示,真实生产 LLM 应该用 sub-agent 的 tools:
```java
// 真实 LLM 路径(章节 14 工具调用进阶)
orchestrator.prompt()
    .user(userRequest)
    .tools(researcherTool, writerTool, reviewerTool)  // 把 sub-agent 包成工具
    .call()
    .content();
```

### 坑 3: 角色 system prompt 互相串味

每个 sub-agent 的 system prompt 必须**角色隔离**,不要让 researcher 顺便写文章:
```java
// ❌ researcher 既要事实又要写
defaultSystem("你是 Researcher,收集事实,然后顺手写段话")

// ✅ 严格隔离
defaultSystem("你是 Researcher,只输出事实清单")
```

## 0 网络测试

3 tests:
- `Bean 装配` — ApplicationContext 加载 4 个 ChatClient
- `runPipeline 返回结构化结果` — 验证 facts/draft/review/finalAnswer 都有内容
- `OrchestrationService 构造` — 验证 @Qualifier 装配

## 实战清单

- [x] 4 个独立 ChatClient Bean
- [x] `@Qualifier` 注入
- [x] 编程式 Orchestration Service
- [x] 0 网络 mock 路径
- [ ] **生产补 1**:Orchestrator 把 sub-agent 注册成 tools(章节 14 进阶)
- [ ] **生产补 2**:并行调用 sub-agent(节省时间)
- [ ] **生产补 3**:sub-agent 失败 fallback

## 完整代码

[03-agent/16-multi-agent/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/03-agent/16-multi-agent)

## 学完下一步

[17 Spring AI Graph →](17-spring-ai-graph.md) — 用状态机 + 条件边表达更复杂的 Agent 流程(多分支 / 循环 / 跳过)。
