package cc.misshi.springai.graph;

import java.util.Map;

/**
 * Graph 节点 — 第 17 章 spring-ai-graph 核心抽象.
 *
 * <p>每个节点接收 State(Map),返回更新后的 State.
 * 状态在节点间流转,边的方向决定流转顺序.
 *
 * <p>用 abstract class 而非 @FunctionalInterface,因为节点需要 id() + doExecute() 两个抽象方法.
 */
public abstract class GraphNode {

    /**
     * 节点 ID,在图中唯一.
     */
    public abstract String id();

    /**
     * 实际处理逻辑:接收 state,返回新的 state.
     */
    public abstract Map<String, Object> doExecute(Map<String, Object> state);
}
