package cc.misshi.springai.reranking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 简单关键词重排序:对每个 doc 算 query 关键词命中率,重排序。
 *
 * <p>真实生产用 Cohere Rerank API / BGE Reranker(via TransformersEmbeddingModel),
 * 本章用关键词版做 demo,展示 DocumentPostProcessor 接口。
 */
@Component
public class KeywordRerankProcessor implements DocumentPostProcessor {

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        String[] keywords = query.text().toLowerCase().split("\\s+");
        Map<Document, Double> scores = new HashMap<>();
        for (Document doc : documents) {
            scores.put(doc, score(doc, keywords));
        }
        return documents.stream()
                .sorted((a, b) -> Double.compare(scores.get(b), scores.get(a)))
                .toList();
    }

    private double score(Document doc, String[] keywords) {
        String text = doc.getText().toLowerCase();
        double score = 0.0;
        for (String k : keywords) {
            if (k.length() < 2) continue;  // 跳过单字
            if (text.contains(k)) {
                score += 1.0;
                // 标题 / 开头命中加权
                if (text.indexOf(k) < 100) score += 0.5;
            }
        }
        return score;
    }
}
