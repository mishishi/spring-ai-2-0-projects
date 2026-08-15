# 第 17 章 · Spring AI Graph(状态机)


## 一句话总结

**Graph = 节点 + 边 + 条件分支** — 复杂 Agent 流程不再 if-else 嵌套,用有向图 + 状态流转表达"多分支 / 循环 / 跳过"。

## 你将学到

- ✅ `Graph` / `GraphNode` 抽象(状态机)
- ✅ `addNode` / `addEdge` / `addConditionalEdge` API
- ✅ State 在节点间流转(Map<String, Object>)
- ✅ 文档审核 Graph 实战(条件路由)
- ✅ 0 网络测试(纯本地状态机)

## 快速开始

```bash
cd 03-agent/17-spring-ai-graph
mvn test                          # 0 网络 4 tests
mvn spring-boot:run

curl -X POST http://localhost:8080/graph/review \
  -H 'Content-Type: application/json' \
  -d '{"text": "这份报告包含机密信息,请勿外传"}'
```

## 关键 API

### 1. `Graph` 状态机核心

```java
public class Graph {
    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Map<String, List<Edge>> edges = new HashMap<>();
    private final String startNodeId;
    public static final String END = "__END__";

    public Graph addNode(GraphNode node) { ... }
    public Graph addEdge(String from, String to) { ... }
    public Graph addConditionalEdge(String from, String to, Predicate<Map<String, Object>> predicate) { ... }

    public Map<String, Object> run(Map<String, Object> initialState, int maxSteps) { ... }
}
```

**特性**:
- 节点 = 接收 State,返回新 State 的函数
- 普通边 = 总是走向下一节点
- 条件边 = 根据 State 决定走哪条分支
- 状态流转有 `maxSteps` 上限,防死循环

### 2. `GraphNode` 抽象

```java
public abstract class GraphNode {
    public abstract String id();
    public abstract Map<String, Object> doExecute(Map<String, Object> state);
}
```

**为什么用 `abstract class` 而不是 `@FunctionalInterface`?**
- 需要 `id() + doExecute()` 两个抽象方法
- 节点不一定都是 lambda(可能注入 Spring Bean)
- FunctionalInterface 只允许 1 个 abstract method

### 3. 文档审核 Graph 例子

```java
@Bean
public Graph documentReviewGraph() {
    Graph graph = new Graph("extract");

    graph.addNode(new GraphNode() {
        public String id() { return "extract"; }
        public Map<String, Object> doExecute(Map<String, Object> state) {
            String text = (String) state.getOrDefault("text", "");
            state.put("summary", text.substring(0, Math.min(50, text.length())));
            return state;
        }
    });

    graph.addNode(new GraphNode() {
        public String id() { return "checkKeywords"; }
        public Map<String, Object> doExecute(Map<String, Object> state) {
            String text = (String) state.getOrDefault("text", "");
            boolean hasSensitive = text.contains("机密") || text.contains("密码");
            state.put("hasSensitive", hasSensitive);
            return state;
        }
    });

    graph.addNode(new GraphNode() {
        public String id() { return "escalation"; }
        public Map<String, Object> doExecute(Map<String, Object> state) {
            state.put("decision", "ESCALATE");
            state.put("reason", "检测到敏感词,需要人工 review");
            return state;
        }
    });

    graph.addNode(new GraphNode() {
        public String id() { return "publish"; }
        public Map<String, Object> doExecute(Map<String, Object> state) {
            state.put("decision", "PUBLISH");
            state.put("reason", "内容安全,可发布");
            return state;
        }
    });

    graph.addEdge("extract", "checkKeywords");
    graph.addConditionalEdge("checkKeywords", "escalation",
            state -> (boolean) state.getOrDefault("hasSensitive", false));
    graph.addConditionalEdge("checkKeywords", "publish",
            state -> !(boolean) state.getOrDefault("hasSensitive", false));
    graph.addEdge("escalation", Graph.END);
    graph.addEdge("publish", Graph.END);

    return graph;
}
```

**流程**:
```
       ┌─────────┐
START→ │ extract │
       └────┬────┘
            ▼
   ┌─────────────────┐
   │ checkKeywords    │
   └────┬──────┬─────┘
        │      │
  hasSensitive?
   ┌────▼──┐  ┌──▼────┐
   │ESCAL. │  │PUBLISH│
   └────┬──┘  └──┬────┘
        │        │
        ▼        ▼
       END      END
```

## 3 个 Demo

### Demo 1: 敏感内容上报

```bash
curl -X POST http://localhost:8080/graph/review \
  -H 'Content-Type: application/json' \
  -d '{"text": "这份报告包含机密信息,请勿外传"}'
```

返回:
```json
{
  "summary": "这份报告包含机密信息,请勿外传",
  "hasSensitive": true,
  "decision": "ESCALATE",
  "reason": "检测到敏感词,需要人工 review"
}
```

### Demo 2: 普通内容发布

```bash
curl -X POST http://localhost:8080/graph/review \
  -H 'Content-Type: application/json' \
  -d '{"text": "今天天气真好,我们去公园散步"}'
```

返回:
```json
{
  "summary": "今天天气真好,我们去公园散步",
  "hasSensitive": false,
  "decision": "PUBLISH",
  "reason": "内容安全,可发布"
}
```

### Demo 3: 多分支 + 循环(扩展)

实际生产:加入 `humanReview` 节点(需要人工确认) → 走回 `publish` 或 `reject`。
```java
graph.addConditionalEdge("escalation", "humanReview", state -> true);
graph.addEdge("humanReview", "publish");  // 人工确认后发布
```

## 踩坑(3 大常见)

### 坑 1: 死循环忘了 `maxSteps`

```java
// ❌ 没有 maxSteps 限制
graph.run(state);  // 永远跑

// ✅ 总是传 maxSteps
graph.run(state, 20);  // 最多 20 步
```

### 坑 2: 条件边 predicate 永远 true

```java
// ❌ 永远走 A,B 永远跑不到
graph.addConditionalEdge("X", "A", state -> true);
graph.addConditionalEdge("X", "B", state -> false);

// ✅ 第一个 true 的边先走,后续跳过
// 设计时确保条件互斥或按优先级
```

### 坑 3: State 类型转换 NPE

```java
// ❌ 直接强转,可能 NPE
String text = (String) state.get("text");

// ✅ getOrDefault + 类型校验
String text = (String) state.getOrDefault("text", "");
```

## 0 网络测试

4 tests:
- `addNode / addEdge` 基础
- `条件边 predicate` 分支
- `run` 状态流转
- `maxSteps` 上限保护

## 实战清单

- [x] Graph 状态机抽象
- [x] 节点 + 边 + 条件边
- [x] State 流转
- [x] maxSteps 死循环保护
- [ ] **生产补 1**:State 持久化(跑超时的图能从断点恢复)
- [ ] **生产补 2**:并行节点(同一层的 node 并行跑)
- [ ] **生产补 3**:可视化(图编辑器 / DOT 输出)

## 完整代码

[03-agent/17-spring-ai-graph/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/03-agent/17-spring-ai-graph)

## 学完下一步

[18 Agent Production →](19-agent-production.md) — ChatMemory / Streaming / 安全 Math / Actuator,生产化最后一公里。
