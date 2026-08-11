package cc.misshi.springai.knowledgehub;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 简易知识库 — Phase 4 项目 5.
 *
 * <p>内存版关键词检索,模拟 RAG(0 网络).
 * <p>真实生产换 SimpleVectorStore + Embedding.
 */
@Component
public class KnowledgeBase {

    private final List<Doc> docs = new ArrayList<>();

    public KnowledgeBase() {
        // 预置知识
        add("公司年假制度: 入职 1 年 5 天,3 年 10 天,5 年 15 天,10 年 20 天");
        add("报销流程: 出差结束 7 天内提交 OA,附发票,主管审批,财务打款");
        add("加班规定: 工作日 1.5 倍,周末 2 倍,节假日 3 倍;每月不超过 36 小时");
        add("远程办公: 每周最多 2 天,需前一天申请,响应时间不超过 1 小时");
        add("Spring AI 是什么: Spring 团队推出的 AI 集成框架,统一 ChatClient/Embedding/VectorStore API");
        add("RAG 全称: Retrieval-Augmented Generation,RAG 检索增强生成,RAG 先检索相关文档再让 LLM 答");
        add("MCP 是什么: Model Context Protocol,Anthropic 提出的模型与工具通信协议");
        add("@Tool 注解: Spring AI 2.0 的工具声明,标在方法上即可被 LLM 调");
        add("ChatMemory: 维护多轮对话上下文,MessageWindowChatMemory 按消息数滑动窗口");
        add("VectorStore: 存 embedding 向量,similaritySearch 返回 top-K 相似文档");
    }

    public void add(String text) {
        docs.add(new Doc("kb-" + docs.size(), text));
    }

    public List<Doc> search(String query, int topK) {
        String[] qTokens = tokenize(query);
        if (qTokens.length == 0) return List.of();
        return docs.stream()
                .map(d -> new Scored(d, score(d.text, qTokens)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingInt((Scored s) -> s.score).reversed())
                .limit(topK)
                .map(s -> s.doc)
                .toList();
    }

    public int size() {
        return docs.size();
    }

    public record Doc(String id, String text) {}

    private record Scored(Doc doc, int score) {}

    // ─── Tokenize(简化版,中文 2-gram + 英文单词) ───────────

    private static String[] tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        StringBuilder english = new StringBuilder();
        StringBuilder chinese = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                if (english.length() > 0) {
                    tokens.add(english.toString());
                    english.setLength(0);
                }
                chinese.append(ch);
            } else if (Character.isLetterOrDigit(ch)) {
                if (chinese.length() >= 2) {
                    tokens.add(chinese.substring(chinese.length() - 2));
                }
                chinese.setLength(0);
                english.append(ch);
            } else {
                if (english.length() > 0) {
                    tokens.add(english.toString());
                    english.setLength(0);
                }
                if (chinese.length() >= 2) {
                    for (int j = 0; j + 1 < chinese.length(); j += 2) {
                        tokens.add(chinese.substring(j, j + 2));
                    }
                }
                chinese.setLength(0);
            }
        }
        if (english.length() > 0) tokens.add(english.toString());
        if (chinese.length() >= 2) {
            for (int j = 0; j + 1 < chinese.length(); j += 2) {
                tokens.add(chinese.substring(j, j + 2));
            }
        }
        return tokens.stream().filter(t -> t.length() >= 2).toArray(String[]::new);
    }

    private static int score(String text, String[] qTokens) {
        String lower = text.toLowerCase();
        int score = 0;
        for (String t : qTokens) {
            String tLower = t.toLowerCase();
            int idx = 0;
            while ((idx = lower.indexOf(tLower, idx)) != -1) {
                score++;
                idx += tLower.length();
            }
        }
        return score;
    }
}
