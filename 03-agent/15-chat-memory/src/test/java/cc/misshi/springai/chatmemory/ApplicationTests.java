package cc.misshi.springai.chatmemory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.ai.openai.api-key=fake-key")
class ApplicationTests {
    @Test void contextLoads() {}
}
