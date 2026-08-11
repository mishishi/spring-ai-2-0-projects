# P2 · 企业文档问答助手


## 一句话总结

**关键词检索 RAG(0 网络)+ ChatClient 真 LLM 路径** — 内部 FAQ / 制度查询自动化,中文 2-gram 分词 + 模板化答案兜底。

## 你将学到

- ✅ RAG 完整流程(Load → 检索 → 答案)
- ✅ 中文 2-gram 分词(避单字命中噪音)
- ✅ `@PostConstruct` 启动时加载文档
- ✅ `Resource[]` 通配符注入(`classpath:sample-docs/*.md`)
- ✅ 0 mockito,纯 Java 集合做测试

## 快速开始

```bash
cd 04-projects/project-2-doc-qa
mvn test                          # 0 网络 6 tests, ~6s
mvn spring-boot:run

curl -X POST http://localhost:8080/qa/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "怎么报销?"}'
```

## 架构

```
                    ┌──────────────────┐
                    │ @PostConstruct 启动时
                    │ 加载 sample-docs/*.md
                    │  按段落切块
                    │  tokenize (中文 2-gram)
                    └────────┬──────────┘
                             │
                             ▼
  POST /qa/ask         ┌──────────────────┐
  { "question": ... } ──▶ DocQaService     │
                        │  1. tokenize query
                        │  2. 关键词评分
                        │  3. top-3 段落
                        │  4. 模板化答案
                        └────────┬──────────┘
                                 │ 答案 + 引用
                                 ▼
                          "【命中 1 个文档段落】
                           > [handbook.md] 报销流程..."
```

## 关键代码

### 1. 启动时加载文档

```java
@PostConstruct
public void loadDocsOnStartup() throws IOException {
    for (Resource doc : docs) {
        String content = doc.getContentAsString(StandardCharsets.UTF_8);
        for (String paragraph : content.split("\\n\\n+")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;
            chunks.add(new DocChunk(doc.getFilename(), trimmed));
        }
    }
    log.info("加载 {} 个文档块(来自 {} 个文件)", chunks.size(), docs.length);
}
```

**关键**:
- `Resource[] docs` 由 Spring 注入 `classpath:sample-docs/*.md`(通配符)
- 按段落切块(`\\n\\n+` 双换行)
- 启动时一次加载,运行期不再读文件

### 2. 中文 2-gram 分词

```java
private static String[] tokenize(String s) {
    List<String> tokens = new ArrayList<>();
    StringBuilder english = new StringBuilder();
    StringBuilder chinese = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        if (ch >= '\u4e00' && ch <= '\u9fff') {
            if (english.length() > 0) { tokens.add(english.toString()); english.setLength(0); }
            chinese.append(ch);
        } else if (Character.isLetterOrDigit(ch)) {
            if (chinese.length() >= 2) { tokens.add(chinese.substring(chinese.length() - 2)); }
            chinese.setLength(0);
            english.append(ch);
        } else {
            // 标点:flush english + 2-gram 切 chinese
            if (english.length() > 0) { tokens.add(english.toString()); english.setLength(0); }
            if (chinese.length() >= 2) {
                for (int j = 0; j + 1 < chinese.length(); j += 2) {
                    tokens.add(chinese.substring(j, j + 2));
                }
            }
            chinese.setLength(0);
        }
    }
    // flush 末尾
    if (english.length() > 0) tokens.add(english.toString());
    if (chinese.length() >= 2) {
        for (int j = 0; j + 1 < chinese.length(); j += 2) {
            tokens.add(chinese.substring(j, j + 2));
        }
    }
    return tokens.stream().filter(t -> t.length() >= 2).toArray(String[]::new);
}
```

**为什么 2-gram?** 单字("天"/"是")噪音大,几乎任何中文 doc 都含;2-gram("什么"/"报销")精准。**英文单词整词保留**。

### 3. 关键词评分检索

```java
List<DocChunk> search(String query, int topK) {
    String[] qTokens = tokenize(query);
    return chunks.stream()
            .map(c -> new ScoredChunk(c, score(c.text(), qTokens)))
            .filter(sc -> sc.score > 0)
            .sorted(Comparator.comparingInt((ScoredChunk sc) -> sc.score).reversed())
            .limit(topK)
            .map(sc -> sc.chunk)
            .toList();
}

private static int score(String text, String[] qTokens) {
    String lower = text.toLowerCase();
    int s = 0;
    for (String t : qTokens) {
        String tLower = t.toLowerCase();
        int idx = 0;
        while ((idx = lower.indexOf(tLower, idx)) != -1) {
            s++;
            idx += tLower.length();
        }
    }
    return s;
}
```

**核心**:
- query 里的每个 token,在 doc 里出现次数累加
- score=0 过滤(完全无关)
- 按 score 降序,取 top-K

### 4. 模板化答案

```java
public String ask(String question) {
    List<DocChunk> top = search(question, 3);
    if (top.isEmpty()) {
        return "我不知道,文档里没有找到相关内容。";
    }
    StringBuilder sb = new StringBuilder("【命中 ").append(top.size()).append(" 个文档段落】\n\n");
    for (DocChunk c : top) {
        sb.append("> [").append(c.file()).append("] ").append(c.text()).append("\n\n");
    }
    sb.append("【答案】");
    if (top.get(0).text().contains("报销")) sb.append("请参考上述报销流程。");
    else if (top.get(0).text().contains("年假")) sb.append("请查阅上述假期制度。");
    return sb.toString();
}
```

规则化兜底:根据 top-1 关键词判断类型,给模板答案。真实 LLM 时改成"拼 prompt → ChatClient.call()"。

## 3 个 Demo

### Demo 1: 命中关键词

```bash
curl -X POST http://localhost:8080/qa/ask -d '{"question":"怎么报销?"}'
```

返回引用"报销流程"段落 + 答案"请参考上述报销流程"。

### Demo 2: 关键词不命中

```bash
curl -X POST http://localhost:8080/qa/ask -d '{"question":"今天星期几?"}'
```

返回"我不知道,文档里没有找到相关内容"。

### Demo 3: 真实 LLM 跑

```bash
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
# 同样 curl,但 ChatClient 不为 null,LLM 基于 top-3 段落 + 问题,产出更自然答案
```

## 关键技术决策:为什么不用 VectorStore

**Spring AI 2.0 砍了 `spring-ai-vector-store-simple` starter**(1.x 还在),只剩 `spring-ai-pgvector-store`(需要 Docker Postgres)。

0 网络要求下,选择:
1. **关键词检索**(本项目) — 中文 2-gram + 模板化,0 依赖
2. **SimpleVectorStore + Embedding** — 1.1.3 风格,需要真 LLM(不满足 0 网络)
3. **PgVector + Docker** — 真实持久化,见 Phase 2 chapter 08

## 踩坑(3 大常见)

### 坑 1: 切块边界吞掉内容

```java
// ❌ 按 \n 切,段落内的换行被破坏
content.split("\n");

// ✅ 按段落切(\n\n+)
content.split("\\n\\n+");
```

### 坑 2: 单字 token 噪音

```java
// ❌ "天气怎么样" 切 ["天","气","怎","么","怎","样"], "天" 命中任何 doc
tokenize("天气怎么样");

// ✅ 2-gram, "天气" 整体匹配
filter(t -> t.length() >= 2);
```

### 坑 3: 大小写不一致

```java
// ❌ query "RAG" 搜 doc "rag 全称" 找不到
String lower = text.toLowerCase();
lower.indexOf("RAG");  // 大小写敏感

// ✅ 统一 lowercase
String tLower = t.toLowerCase();
lower.indexOf(tLower);
```

## 0 网络测试

6 tests:
- 段落切块
- 中英文分词
- top-K 排序
- 答案模板匹配
- 空 query
- 多 doc 跨文件命中

## 实战清单

- [x] 启动时加载文档
- [x] 中文 2-gram 分词
- [x] 关键词评分检索
- [x] 模板化答案
- [x] 0 网络 + 真 LLM 双路径
- [ ] **生产补 1**:换 SimpleVectorStore + Embedding
- [ ] **生产补 2**:多租户 + 文档权限
- [ ] **生产补 3**:DocumentReader(MD/PDF/HTML,章节 09)

## 完整代码

[04-projects/project-2-doc-qa/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/04-projects/project-2-doc-qa)

## 学完下一步

[P3 AI 旅行规划师 →](project-3-travel-planner.md) — Multi-Agent 编排,4 个 sub-agent + 1 orchestrator。
