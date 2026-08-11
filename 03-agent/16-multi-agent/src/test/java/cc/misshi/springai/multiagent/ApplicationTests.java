package cc.misshi.springai.multiagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.ai.openai.api-key=fake-key")
class ApplicationTests {
    @Test void contextLoads() {}
}
