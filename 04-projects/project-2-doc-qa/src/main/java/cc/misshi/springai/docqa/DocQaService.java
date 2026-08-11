package cc.misshi.springai.docqa;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 企业文档问答服务 — Phase 4 项目 2.
 *
 * <p>0 网络设计:
 * <ul>
 *   <li>启动时把 sample-docs/*.md 加载到内存</li>
 *   <li>用关键词匹配做"检索"(避开了 VectorStore / EmbeddingModel 依赖)</li>
 *   <li>ChatClient 为 null 时走 mock 模板(0 LLM 调用)</li>
 * </ul>
 * 真实生产:换 {@code SimpleVectorStore} + OpenAI Embedding + ChatClient.prompt()。
 */
@Service
public class DocQaService {

    private static final Logger log = LoggerFactory.getLogger(DocQaService.class);

    private final ChatClient chatClient;
    private final List<DocChunk> chunks = new ArrayList<>();
    private final Resource[] docs;

    public DocQaService(ChatClient.Builder builder,
                        @Value("classpath:sample-docs/*.md") Resource[] docs) {
        this.docs = docs;
        this.chatClient = (builder == null) ? null : builder
                .defaultSystem("""
                        你是企业知识助手,严格根据提供的文档回答。
                        如果文档里没有相关信息,直接说"我不知道",不要编造。
                        回答简洁,引用的文档段落用 markdown blockquote 引用。
                        """)
                .build();
    }

    @PostConstruct
    public void loadDocsOnStartup() throws IOException {
        for (Resource doc : docs) {
            String content = doc.getContentAsString(StandardCharsets.UTF_8);
            // 简单按段落切块(每段作为一个 chunk)
            for (String paragraph : content.split("\\n\\n+")) {
                String trimmed = paragraph.trim();
                if (trimmed.isEmpty()) continue;
                String fileName = doc.getFilename();
                chunks.add(new DocChunk(fileName, trimmed));
            }
        }
        log.info("加载 {} 个文档块(来自 {} 个文件)", chunks.size(), docs.length);
    }

    /**
     * 回答问题:关键词检索 top-3 + 模板式回答。
     */
    public String ask(String question) {
        if (question == null || question.isBlank()) {
            return "问题不能为空。";
        }

        // 1. 关键词检索
        List<DocChunk> top = search(question, 3);
        if (top.isEmpty()) {
            return "我不知道,文档里没有找到相关内容。";
        }

        // 2. 拼答案
        StringBuilder sb = new StringBuilder();
        sb.append("【命中 ").append(top.size()).append(" 个文档段落】\n\n");
        for (int i = 0; i < top.size(); i++) {
            DocChunk c = top.get(i);
            sb.append("> [").append(c.file()).append("] ").append(c.text()).append("\n\n");
        }
        sb.append("【答案】根据文档,");
        // 简单规则:用 question 里的关键词在 top[0] 命中,产出模板化答案
        String firstText = top.get(0).text();
        if (firstText.contains("报销")) sb.append("请参考上述报销流程。");
        else if (firstText.contains("年假") || firstText.contains("假期")) sb.append("请查阅上述假期制度。");
        else if (firstText.contains("加班")) sb.append("请查阅上述加班制度。");
        else if (firstText.contains("远程") || firstText.contains("在家")) sb.append("请查阅上述远程办公制度。");
        else sb.append("请参考上述相关文档段落。");

        return sb.toString();
    }

    /**
     * 关键词评分检索:query 里的词在 chunk 里出现的次数作为分数。
     */
    List<DocChunk> search(String query, int topK) {
        String[] qTokens = tokenize(query);
        if (qTokens.length == 0) return List.of();

        return chunks.stream()
                .map(c -> new ScoredChunk(c, score(c.text(), qTokens)))
                .filter(sc -> sc.score > 0)
                .sorted(Comparator.comparingInt((ScoredChunk sc) -> sc.score).reversed())
                .limit(topK)
                .map(sc -> sc.chunk)
                .toList();
    }

    private static String[] tokenize(String s) {
        // 中文 2-gram + 英文单词
        String normalized = s.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        StringBuilder english = new StringBuilder();
        StringBuilder chinese = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                if (english.length() > 0) {
                    tokens.add(english.toString());
                    english.setLength(0);
                }
                chinese.append(ch);
            } else if (Character.isLetterOrDigit(ch)) {
                if (chinese.length() > 0) {
                    // flush 2-gram
                    for (int j = 0; j + 1 < chinese.length(); j += 2) {
                        tokens.add(chinese.substring(j, j + 2));
                    }
                    if (chinese.length() % 2 == 1) {
                        // 末尾单字配下一中文字符(没有就丢弃)
                        tokens.add(chinese.substring(chinese.length() - 1) + " ");
                    }
                    chinese.setLength(0);
                }
                english.append(ch);
            } else {
                flushEnglish(english, tokens);
                flushChinese(chinese, tokens);
            }
        }
        flushEnglish(english, tokens);
        flushChinese(chinese, tokens);

        // 过滤:只保留长度 >= 2 的 token
        return tokens.stream()
                .filter(t -> t.length() >= 2 && !t.endsWith(" "))
                .toArray(String[]::new);
    }

    private static void flushEnglish(StringBuilder english, List<String> tokens) {
        if (english.length() > 0) {
            tokens.add(english.toString());
            english.setLength(0);
        }
    }

    private static void flushChinese(StringBuilder chinese, List<String> tokens) {
        if (chinese.length() >= 2) {
            for (int j = 0; j + 1 < chinese.length(); j += 2) {
                tokens.add(chinese.substring(j, j + 2));
            }
        }
        chinese.setLength(0);
    }

    private static int score(String text, String[] qTokens) {
        String lower = text.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String t : qTokens) {
            int idx = 0;
            while ((idx = lower.indexOf(t, idx)) != -1) {
                score++;
                idx += t.length();
            }
        }
        return score;
    }

    record DocChunk(String file, String text) {}

    private record ScoredChunk(DocChunk chunk, int score) {}
}
