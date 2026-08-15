package cc.misshi.springai.graph;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Graph 单元测试 — 0 网络,验证状态机逻辑.
 */
class GraphTest {

    private Graph buildDocumentGraph() {
        Graph g = new Graph("extract");
        g.addNode(new GraphNode() {
            public String id() { return "extract"; }
            public Map<String, Object> doExecute(Map<String, Object> s) {
                s.put("summary", "extracted");
                return s;
            }
        });
        g.addNode(new GraphNode() {
            public String id() { return "check"; }
            public Map<String, Object> doExecute(Map<String, Object> s) {
                s.put("isClean", !((String) s.get("text")).contains("BAD"));
                return s;
            }
        });
        g.addNode(new GraphNode() {
            public String id() { return "publish"; }
            public Map<String, Object> doExecute(Map<String, Object> s) {
                s.put("action", "PUBLISHED");
                return s;
            }
        });
        g.addNode(new GraphNode() {
            public String id() { return "block"; }
            public Map<String, Object> doExecute(Map<String, Object> s) {
                s.put("action", "BLOCKED");
                return s;
            }
        });
        g.addEdge("extract", "check");
        g.addConditionalEdge("check", "publish", s -> Boolean.TRUE.equals(s.get("isClean")));
        g.addConditionalEdge("check", "block", s -> !Boolean.TRUE.equals(s.get("isClean")));
        return g;
    }

    @Test
    void cleanTextShouldRouteToPublish() {
        var state = new HashMap<String, Object>();
        state.put("text", "正常内容,没问题");
        var result = buildDocumentGraph().run(state, 10);
        assertThat(result.get("action")).isEqualTo("PUBLISHED");
    }

    @Test
    void badTextShouldRouteToBlock() {
        var state = new HashMap<String, Object>();
        state.put("text", "这是 BAD 内容");
        var result = buildDocumentGraph().run(state, 10);
        assertThat(result.get("action")).isEqualTo("BLOCKED");
    }

    @Test
    void stateShouldPassThroughNodes() {
        var state = new HashMap<String, Object>();
        state.put("text", "正常");
        var result = buildDocumentGraph().run(state, 10);
        assertThat(result).containsKeys("text", "summary", "isClean", "action", "__step__");
        assertThat((Integer) result.get("__step__")).isEqualTo(2);
    }
}
