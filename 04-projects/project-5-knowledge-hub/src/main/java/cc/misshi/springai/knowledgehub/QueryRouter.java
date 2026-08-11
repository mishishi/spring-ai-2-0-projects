package cc.misshi.springai.knowledgehub;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 路由器 — 根据 query 关键词决定交给哪个 handler.
 *
 * <p>0 网络:纯规则匹配.
 * <p>真实 LLM 时:用 ChatClient 做意图分类 (project-5 的真实 LLM 路径).
 */
@Component
public class QueryRouter {

    public enum Route {
        DOC_QA,        // 文档问答 → RAG
        CODE_REVIEW,   // 代码审查 → @Tool
        WEEKLY_REPORT, // 周报生成 → ChatClient
        CHITCHAT,      // 闲聊 → 直接回答
        UNKNOWN
    }

    public Route route(String query) {
        if (query == null || query.isBlank()) return Route.UNKNOWN;
        String lower = query.toLowerCase(Locale.ROOT);

        // 1. 代码审查
        if (containsAny(lower, List.of("代码", "code", "review", "审查", "bug", "重构", "function", "method", "class"))) {
            return Route.CODE_REVIEW;
        }
        // 2. 周报
        if (containsAny(lower, List.of("周报", "weekly", "总结", "summary", "本周", "这周"))) {
            return Route.WEEKLY_REPORT;
        }
        // 3. 文档问答(常见问题词)
        if (containsAny(lower, List.of("怎么", "如何", "什么", "是什么", "哪里", "何时", "谁", "why", "how", "what", "where", "who", "when", "?", "？"))) {
            return Route.DOC_QA;
        }
        // 4. 默认闲聊
        if (containsAny(lower, List.of("你好", "hi", "hello", "在吗", "嗨"))) {
            return Route.CHITCHAT;
        }
        return Route.DOC_QA; // fallback
    }

    private boolean containsAny(String s, List<String> keys) {
        for (String k : keys) {
            if (s.contains(k)) return true;
        }
        return false;
    }
}
