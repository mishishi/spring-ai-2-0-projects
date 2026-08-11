package cc.misshi.springai.agentprod;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 记忆管理 — 第 18 章 Agent 生产化.
 *
 * <p>Spring AI 2.0 提供:
 * <ul>
 *   <li>{@link ChatMemory} — 抽象接口</li>
 *   <li>{@link MessageWindowChatMemory} — 保留最近 N 条消息(生产推荐)</li>
 *   <li>未来: JDBC / Redis 持久化(社区实现)</li>
 * </ul>
 *
 * <p>用法: 配合 {@code MessageChatMemoryAdvisor} 自动注入到 ChatClient.
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 保留最近 10 条消息的窗口.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
    }
}
