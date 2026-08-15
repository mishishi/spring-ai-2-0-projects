# 第 15 章 · Chat Memory 多轮对话


## 你将学到

- ✅ 为什么 LLM 默认"不记事" + Chat Memory 解决什么
- ✅ `ChatMemory` 接口 + `MessageWindowChatMemory` 实现
- ✅ 4 种 `ChatMemoryRepository`:InMemory / JDBC / Cassandra / Neo4j
- ✅ `MessageChatMemoryAdvisor` 自动注入 memory 到 prompt
- ✅ `conversationId` 多用户隔离
- ✅ 4 个实战场景:基线对比 / 滑动窗口 / 自动注入 / 多用户

## 一句话总结

LLM 默认 stateless(每次调用独立),Chat Memory 通过 `ChatMemory` 接口保留对话历史,`MessageChatMemoryAdvisor` 自动把 history 拼进 prompt,让 LLM "记得"上文。

## 读者学完能做什么

- 理解 Chat Memory 在 LLM 调用链中的位置
- 选 ChatMemoryRepository(开发用 InMemory,生产用 JDBC)
- 用 `MessageChatMemoryAdvisor` 简化多轮对话代码
- 按 conversationId 隔离多用户
- 选窗口大小(默认 10 条,长对话 20-30)
- 处理 token 增长问题(MessageTokenWindowChatMemory)

## 5 分钟上手

```bash
export OPENAI_API_KEY=sk-xxxxx
cd 03-agent/15-chat-memory
mvn spring-boot:run
```

跑 4 个 demo:
1. 无 memory 对比基线(LLM 不记得上文)
2. MessageWindowChatMemory(滑动窗口)
3. MessageChatMemoryAdvisor(自动注入)
4. conversationId 多用户隔离

## 为什么需要 Chat Memory(背景)

LLM 是 **stateless**(无状态):每次 `client.prompt().user(q).call()` 是独立调用,**完全不知道上次说过什么**。

```java
client.prompt().user("我叫张三").call().content();   // "你好张三!"
client.prompt().user("我叫什么?").call().content();  // "我不清楚你叫什么" ← 没记住!
```

**生产场景的痛点**:
- 客服机器人:用户问 5 句,机器人每句都"重新开始"
- 编程助手:用户说"修复这个 bug",助手问"什么 bug?"
- 旅行规划:用户说"去日本",助手说"好的",但下一句"几天合适"又忘了

**Chat Memory 解法**:
```
[用户: 我叫张三]
    ↓ (写到 memory)
[LLM: 你好张三!]
    ↓ (写 assistant 回复到 memory)
[用户: 我叫什么?]
    ↓ (chat memory 把 history 拼进 prompt: User:我叫张三, Assistant:你好张三, User:我叫什么?)
[LLM: 你叫张三]   ← 记得!
```

**类比**:
- 老 LLM = 失忆患者(每次见面都重头开始)
- Chat Memory = 患者带了笔记本(每次见面前翻一下上次聊的)

## 关键概念(4 个)

### 概念 1:`ChatMemory` 接口

Spring AI 2.0 抽象的对话记忆接口:

```java
public interface ChatMemory {
    String CONVERSATION_ID = "chat_memory_conversation_id";

    void add(String conversationId, List<Message> messages);
    List<Message> get(String conversationId);
    void clear(String conversationId);
}
```

**实现**:

| 实现 | 行为 |
|---|---|
| `MessageWindowChatMemory` | 滑动窗口,保留最近 N 条(默认全 N) |
| `MessageTokenWindowChatMemory` | 按 token 数窗口(避免超上下文) |

### 概念 2:`MessageWindowChatMemory`

最常用的实现,**保留最近 N 条消息**:

```java
ChatMemory memory = MessageWindowChatMemory.builder()
    .maxMessages(10)         // 保留最近 10 条
    .build();
```

**N 怎么选**:
- 简单 FAQ:10 条够
- 复杂多轮:20-30 条
- 客服场景:50 条(用户长对话)
- **过大风险**:N 太大 → 每次调用拼进 prompt 的 history 太长 → token 烧钱

### 概念 3:`ChatMemoryRepository` 存储

存储后端抽象:

```java
public interface ChatMemoryRepository {
    List<Message> findByConversationId(String conversationId);
    void saveAll(String conversationId, List<Message> messages);
    void deleteByConversationId(String conversationId);
}
```

**4 种实现**:

| Repository | 适用 | 持久化 |
|---|---|---|
| `InMemoryChatMemoryRepository` | 开发 / 测试 | ❌ 重启丢 |
| `JdbcChatMemoryRepository` | **生产推荐** | ✅ PostgreSQL / MySQL |
| `CassandraChatMemoryRepository` | 大规模分布式 | ✅ Cassandra |
| `Neo4jChatMemoryRepository` | 图查询(关系分析) | ✅ Neo4j |

**生产选 JDBC**:
```java
ChatMemoryRepository repo = JdbcChatMemoryRepository.builder()
    .jdbcTemplate(jdbcTemplate)
    .dialect(JdbcChatMemoryRepositoryDialect.POSTGRES)
    .build();

ChatMemory memory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(repo)
    .maxMessages(20)
    .build();
```

### 概念 4:`MessageChatMemoryAdvisor` 自动注入

把 ChatMemory 包装成 Advisor,**自动**把 history 拼进 prompt:

```java
ChatClient client = builder
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
    .build();

// 不用手动管 history,Advisor 自动:
// 1. 从 memory 读 history
// 2. 拼到 user message 前面
// 3. 调 LLM
// 4. 把 user + assistant 写回 memory
String r = client.prompt()
    .user("我叫张三")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-1"))    // 关键:传 conversationId
    .call()
    .content();
```

**`conversationId` 是 key**:不同用户用不同 id,history 自动隔离。

## 4 个实战场景

### 场景 1:无 memory(对比基线)

```java
ChatClient client = builder.defaultSystem("你是助手").build();

client.prompt().user("我叫张三").call().content();      // "你好张三"
client.prompt().user("我叫什么?").call().content();    // "我不清楚"
```

LLM **不记得**上文。

### 场景 2:手动 ChatMemory(理解原理)

```java
ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10).build();

// 手动写
memory.add("user-1", List.of(UserMessage.builder().text("我叫张三,30 岁").build()));
memory.add("user-1", List.of(AssistantMessage.builder().content("你好张三!").build(),
                              UserMessage.builder().text("我多大了?").build()));

// 手动读 + 拼
List<Message> history = memory.get("user-1");
List<Message> fullMessages = new ArrayList<>(history);
fullMessages.add(UserMessage.builder().text("我叫什么?").build());
String r = client.prompt().messages(fullMessages).call().content();
```

**优点**:完全控制。**缺点**:每个调用都要手动 add/get,繁琐。

### 场景 3:MessageChatMemoryAdvisor(推荐)

```java
ChatClient client = builder
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
    .build();

client.prompt()
    .user("我叫张三,30 岁,Java 工程师")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-1"))
    .call()
    .content();
// → "你好张三!..."

client.prompt()
    .user("我叫什么?做什么的?")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-1"))
    .call()
    .content();
// → "你叫张三,Java 工程师"  ← 记得!
```

**Advisor 自动**:
- before:从 memory 读 history → 拼到 prompt
- after:把 user + assistant 回复 → 写回 memory

### 场景 4:多用户隔离(conversationId)

```java
// user-1 的对话
client.prompt().user("我叫张三")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-1"))
    .call().content();

// user-2 的对话
client.prompt().user("我叫李四")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-2"))
    .call().content();

// user-1 再问"我叫什么?" → 张三
// user-2 再问"我叫什么?" → 李四
```

**`conversationId` 是隔离 key**,通常用 userId / sessionId / threadId。

## 工作流程图

```
client.prompt().user(q).call()
    ↓
[MessageChatMemoryAdvisor.before()]
   1. 读 context[conversationId]
   2. 从 memory 读 history
   3. 把 history 拼到 user message 前面
    ↓
LLM call(prompt = system + history + user q)
    ↓
LLM 返回 content
    ↓
[MessageChatMemoryAdvisor.after()]
   1. 把 user q + assistant 回复 → 写到 memory[conversationId]
   2. 滑动窗口(超过 N 条丢旧的)
    ↓
return content
```

## 4 种 ChatMemoryRepository 选型

| 场景 | Repository | 理由 |
|---|---|---|
| 单元测试 | `InMemoryChatMemoryRepository` | 0 配置,0 依赖 |
| Demo / 学习 | `InMemoryChatMemoryRepository` | 简单 |
| 生产(单实例) | `JdbcChatMemoryRepository` | 持久化,事务一致 |
| 生产(分布式) | `CassandraChatMemoryRepository` | 高可用,水平扩展 |
| 图分析(用户关系) | `Neo4jChatMemoryRepository` | Cypher 查询 |

**`JdbcChatMemoryRepository` 表结构**(自动建):

```sql
CREATE TABLE SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL,  -- USER / ASSISTANT / SYSTEM / TOOL
    timestamp TIMESTAMP NOT NULL
);
```

## Token 窗口 vs 消息窗口

| 维度 | `MessageWindowChatMemory` | `MessageTokenWindowChatMemory` |
|---|---|---|
| 限制维度 | 消息**数** | token **数** |
| 优点 | 简单 | 精确控制 token |
| 缺点 | 1 条长消息 = 1 条但占 5000 token | 配置复杂(要 token 计数) |
| 适用 | 消息长度均匀 | 消息长度差异大 |

```java
// Token 窗口
MessageTokenWindowChatMemory.builder()
    .maxTokens(4000)         // GPT-3.5 上下文的一半
    .build();
```

## 测试(纯本地 0 网络)

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.ai.openai.api-key=fake-key")
class ApplicationTests {
    @Test void contextLoads() {}

    @Test
    void testMemoryAddAndGet() {
        ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10).build();
        memory.add("user-1", List.of(UserMessage.builder().text("hi").build()));
        List<Message> history = memory.get("user-1");
        assertThat(history).hasSize(1);
    }

    @Test
    void testWindowTrim() {
        ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(2).build();
        memory.add("u1", List.of(UserMessage.builder().text("m1").build()));
        memory.add("u1", List.of(UserMessage.builder().text("m2").build()));
        memory.add("u1", List.of(UserMessage.builder().text("m3").build()));    // 触发窗口
        assertThat(memory.get("u1")).hasSize(2);                                  // m1 丢了
    }
}
```

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| 忘了传 `conversationId` | 所有用户共享同一个 history,串台 | 每次 advisor 必传 |
| 用了 `InMemoryChatMemoryRepository` 但部署多实例 | history 不一致 | 换 `JdbcChatMemoryRepository` |
| `maxMessages` 设太大(>50) | token 爆 + 慢 | 20-30 够用,真要更长换 token 窗口 |
| 没用 `MessageChatMemoryAdvisor` 手动管理 | 代码冗长 + 容易漏 add | 优先用 Advisor |
| `conversationId` 用 timestamp | 每次都新 id,history 全丢 | 用稳定 id(userId/sessionId) |
| memory 持久化但 schema 没建 | 启动报错 | `JdbcChatMemoryRepository` 启动时建表(看 config) |
| 调试时改 history 顺序 | 测试不通过 | memory 是有序 List,append only |
| `MessageTokenWindowChatMemory` 没设 max | 默认是模型上下文,容易超 | 显式设 `maxTokens(4000)` |

## 实战部署清单

- [ ] 选 `ChatMemoryRepository`(生产用 JDBC)
- [ ] 配 `MessageWindowChatMemory`(maxMessages 20-30)
- [ ] 挂 `MessageChatMemoryAdvisor` 到 `defaultAdvisors`
- [ ] 每次 `client.prompt()` 必传 `conversationId`
- [ ] 用 userId / sessionId 作 conversationId(稳定)
- [ ] 多实例部署用 JDBC / Cassandra(共享 storage)
- [ ] 监控:memory 占用 / 窗口大小
- [ ] 大消息场景换 `MessageTokenWindowChatMemory`
- [ ] 隐私:敏感对话不清空?加 `clear()` API + 定期清理
- [ ] `mvn test` 0 网络 PASS

## 完整代码

[03-agent/15-chat-memory/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/03-agent/15-chat-memory)

## 下一步

- [第 16 章 · MCP (Model Context Protocol) →](16-mcp.md)
- [第 13 章 · Agent Basics →](13-agent-basics.md)— 复习 Agent loop
- [Phase 3 总览 →](overviews/phase-3.md)
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
