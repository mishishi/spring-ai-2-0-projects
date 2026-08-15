package cc.misshi.springai.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 简化的 Graph 状态机 — 第 17 章.
 *
 * <p>特性:
 * <ol>
 *   <li>节点(Map state → Map state)</li>
 *   <li>边(source → target)</li>
 *   <li>条件边(根据 state 决定走哪条分支)</li>
 *   <li>从 start 节点开始,按边走到 END 节点</li>
 * </ol>
 */
public class Graph {

    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Map<String, List<Edge>> edges = new HashMap<>();
    private final String startNodeId;
    public static final String END = "__END__";

    public Graph(String startNodeId) {
        this.startNodeId = startNodeId;
        // END 节点:不修改 state,直接返回
        nodes.put(END, new GraphNode() {
            public String id() { return END; }
            public Map<String, Object> doExecute(Map<String, Object> state) { return state; }
        });
    }

    public Graph addNode(GraphNode node) {
        nodes.put(node.id(), node);
        edges.computeIfAbsent(node.id(), k -> new java.util.ArrayList<>());
        return this;
    }

    /**
     * 普通边:从 from 走到 to.
     */
    public Graph addEdge(String from, String to) {
        edges.computeIfAbsent(from, k -> new java.util.ArrayList<>()).add(new Edge(to, state -> true));
        return this;
    }

    /**
     * 条件边:state 满足 predicate 才走 to,否则跳过.
     */
    public Graph addConditionalEdge(String from, String to, Predicate<Map<String, Object>> predicate) {
        edges.computeIfAbsent(from, k -> new java.util.ArrayList<>()).add(new Edge(to, predicate));
        return this;
    }

    /**
     * 从 start 跑图,直到 END 或循环上限.
     */
    public Map<String, Object> run(Map<String, Object> initialState, int maxSteps) {
        Map<String, Object> state = new HashMap<>(initialState);
        String current = startNodeId;
        int step = 0;
        while (!Objects.equals(current, END) && step < maxSteps) {
            GraphNode node = nodes.get(current);
            if (node == null) {
                throw new IllegalStateException("节点未注册: " + current);
            }
            state = node.doExecute(state);
            state.put("__step__", step);
            step++;

            // 找下一节点
            List<Edge> outgoing = edges.getOrDefault(current, List.of());
            String next = END;
            for (Edge edge : outgoing) {
                if (edge.predicate.test(state)) {
                    next = edge.target;
                    break;
                }
            }
            current = next;
        }
        return state;
    }

    public String startNodeId() { return startNodeId; }

    private record Edge(String target, Predicate<Map<String, Object>> predicate) {}
}
