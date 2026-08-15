package cc.misshi.springai.chatmemory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Spring AI 2.0 Chat Memory。
 *
 * <p>本章 4 个 demo:
 * <ol>
 *   <li>基础多轮对话(无 memory,LLM 不记得上文)</li>
 *   <li>MessageWindowChatMemory(滑动窗口,保留最近 N 条)</li>
 *   <li>MessageChatMemoryAdvisor(自动注入到 prompt)</li>
 *   <li>多用户隔离(conversationId 区分)</li>
 * </ol>
 *
 * <p>运行时需要环境变量 {@code OPENAI_API_KEY}。
 */
@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Profile("!test")
    CommandLineRunner chatMemoryDemo(ChatClient.Builder builder) {
        return args -> {
            // ─── Demo 1: 无 memory(对比基线) ─────────────────
            log.info("══════ Demo 1: 无 memory(LLM 不记得上文) ══════");
            ChatClient noMemoryClient = builder
                .defaultSystem("你是助手,回答简短")
                .build();

            String q1 = "我叫张三";
            String a1 = noMemoryClient.prompt().user(q1).call().content();
            log.info("Q: {}", q1);
            log.info("🤖 A1: {}", a1);

            String q2 = "我叫什么?";
            String a2 = noMemoryClient.prompt().user(q2).call().content();
            log.info("Q: {}", q2);
            log.info("🤖 A2: {}", a2);
            log.info("   (期望:不记得,因为无 memory)");

            // ─── Demo 2: MessageWindowChatMemory(滑动窗口) ─────
            log.info("══════ Demo 2: MessageWindowChatMemory(滑动窗口 10 条) ══════");
            ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

            String r1 = "ok";
            memory.add("user-1", List.of(
                org.springframework.ai.chat.messages.UserMessage.builder().text("我叫张三,30 岁").build()));
            log.info("   add user-1: 我叫张三,30 岁 → {}", r1);

            String r2 = "ok";
            memory.add("user-1", List.of(
                org.springframework.ai.chat.messages.AssistantMessage.builder().content("你好张三!").build(),
                org.springframework.ai.chat.messages.UserMessage.builder().text("我多大了?").build()));
            log.info("   add user-1: 助手回复 + 我多大了? → {}", r2);

            List<org.springframework.ai.chat.messages.Message> history = memory.get("user-1");
            log.info("   history size: {}", history.size());
            history.forEach(m -> log.info("   📝 [{}] {}",
                m.getMessageType(), m.getText().substring(0, Math.min(50, m.getText().length()))));

            // ─── Demo 3: MessageChatMemoryAdvisor(自动注入) ───
            log.info("══════ Demo 3: MessageChatMemoryAdvisor(自动注入到 prompt) ══════");
            ChatClient memoryClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .defaultSystem("你是助手,回答简短")
                .build();

            log.info("Q: 我叫李四,25 岁,Java 工程师");
            String mr1 = memoryClient.prompt()
                .user("我叫李四,25 岁,Java 工程师")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-2"))
                .call()
                .content();
            log.info("🤖 A1: {}", mr1);

            log.info("Q: 我叫什么?做什么的?");
            String mr2 = memoryClient.prompt()
                .user("我叫什么?做什么的?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-2"))
                .call()
                .content();
            log.info("🤖 A2: {}", mr2);
            log.info("   (期望:李四,Java 工程师)");

            // ─── Demo 4: 多用户隔离(conversationId) ─────────
            log.info("══════ Demo 4: 多用户隔离(conversationId) ══════");
            log.info("Q (user-3): 我叫王五");
            memoryClient.prompt()
                .user("我叫王五")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-3"))
                .call()
                .content();

            log.info("Q (user-4): 我叫什么?");
            String user4Reply = memoryClient.prompt()
                .user("我叫什么?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-4"))
                .call()
                .content();
            log.info("🤖 A (user-4): {}", user4Reply);
            log.info("   (期望:user-4 不知道,因为没自己的 history)");

            log.info("Q (user-3): 我叫什么?");
            String user3Reply = memoryClient.prompt()
                .user("我叫什么?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-3"))
                .call()
                .content();
            log.info("🤖 A (user-3): {}", user3Reply);
            log.info("   (期望:王五)");
        };
    }
}
