package cc.misshi.springai.knowledgehub;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 多轮对话记忆 — Phase 4 项目 5 简化版.
 *
 * <p>0 网络:用 ArrayDeque 存每 sessionId 的最近 N 条消息.
 * <p>真实 LLM:换 MessageWindowChatMemory (Spring AI 2.0 官方).
 */
@Component
public class ConversationMemory {

    public static final int MAX_MESSAGES = 20;
    private final java.util.Map<String, Deque<Entry>> store = new java.util.concurrent.ConcurrentHashMap<>();

    public void add(String sessionId, String role, String content) {
        Deque<Entry> q = store.computeIfAbsent(sessionId, k -> new ArrayDeque<>(MAX_MESSAGES));
        if (q.size() >= MAX_MESSAGES) q.pollFirst();
        q.addLast(new Entry(role, content));
    }

    public List<Entry> recent(String sessionId) {
        Deque<Entry> q = store.get(sessionId);
        return q == null ? List.of() : new ArrayList<>(q);
    }

    public void clear(String sessionId) {
        store.remove(sessionId);
    }

    public int size(String sessionId) {
        Deque<Entry> q = store.get(sessionId);
        return q == null ? 0 : q.size();
    }

    public record Entry(String role, String content) {}
}
