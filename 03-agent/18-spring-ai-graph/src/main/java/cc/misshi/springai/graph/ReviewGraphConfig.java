package cc.misshi.springai.graph;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 一个文档审核 Graph 例子 — 第 17 章.
 *
 * <p>流程:
 *   输入 → 提取文本(extract) → 检查关键词(checkKeywords) → 条件分支:
 *     - 包含 '机密' → 路由到 escalation(上报) → END
 *     - 其他 → 路由到 publish(发布) → END
 */
@Configuration
public class ReviewGraphConfig {

    @Bean
    public Graph documentReviewGraph() {
        Graph graph = new Graph("extract");

        graph.addNode(new GraphNode() {
            public String id() { return "extract"; }
            public Map<String, Object> doExecute(Map<String, Object> state) {
                String text = (String) state.getOrDefault("text", "");
                // mock:提取前 50 字作为摘要
                String summary = text.length() > 50 ? text.substring(0, 50) + "..." : text;
                state.put("summary", summary);
                state.put("length", text.length());
                return state;
            }
        });

        graph.addNode(new GraphNode() {
            public String id() { return "checkKeywords"; }
            public Map<String, Object> doExecute(Map<String, Object> state) {
                String text = (String) state.getOrDefault("text", "").toString().toLowerCase();
                boolean hasConfidential = text.contains("机密") || text.contains("confidential") || text.contains("secret");
                state.put("hasConfidential", hasConfidential);
                return state;
            }
        });

        graph.addNode(new GraphNode() {
            public String id() { return "publish"; }
            public Map<String, Object> doExecute(Map<String, Object> state) {
                state.put("action", "PUBLISHED");
                state.put("route", "正常发布");
                return state;
            }
        });

        graph.addNode(new GraphNode() {
            public String id() { return "escalation"; }
            public Map<String, Object> doExecute(Map<String, Object> state) {
                state.put("action", "BLOCKED");
                state.put("route", "已上报安全团队");
                return state;
            }
        });

        // 边
        graph.addEdge("extract", "checkKeywords");
        graph.addConditionalEdge("checkKeywords", "publish", state -> !Boolean.TRUE.equals(state.get("hasConfidential")));
        graph.addConditionalEdge("checkKeywords", "escalation", state -> Boolean.TRUE.equals(state.get("hasConfidential")));

        return graph;
    }
}
