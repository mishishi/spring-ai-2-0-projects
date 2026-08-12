# 第 9 章 · Document Loaders 文档加载


## 你将学到

- ✅ 4 种 `DocumentReader`:Markdown / Tika (HTML/PDF/Word) / JSON
- ✅ 4 种 `DocumentSplitter`:Token / Recursive / Semantic / 自定义
- ✅ 按章节切分 vs 按 token 切分选型
- ✅ chunk size / overlap 调优黄金参数
- ✅ 端到端 RAG pipeline(MD/HTML → chunks → pgvector → 问答)
- ✅ 0 网络测试

## 一句话总结

`DocumentReader` 读文件(自动按格式切)→ `DocumentSplitter` 按 token 切块 → `vectorStore.add()` 持久化。Spring AI 一行配置搞定。

## 读者学完能做什么

- 加载 MD / HTML / PDF / Word
- 按 token 切块
- 完整 RAG pipeline
- 选择合适的 Reader / Splitter
- 调 chunk size / overlap 参数
- 给 PDF 加分页、按章节切 MD

## 5 分钟上手

### 1. 跑 ch8 的 Docker pgvector

```bash
docker start pgvector  # 跟 ch8 共用
```

### 2. 准备样例文档

`src/main/resources/sample-docs/`:
- `employee-handbook.md` — 公司手册(Markdown)
- `tech-stack.md` — 技术栈(Markdown)
- `faq.html` — FAQ(HTML)

### 3. 跑

```bash
export OPENAI_API_KEY=sk-xxxxx
cd 02-rag/09-document-loaders
mvn spring-boot:run
```

应用会读 3 个文件 → 按格式切分 → token 切块 → 存到 pgvector → 问 4 个问题演示 RAG。

## 为什么需要 Document Loaders(背景)

RAG 落地时,**文档不是结构化数据**。真实场景:

- 公司 wiki 是 Confluence 导出 HTML
- 产品手册是 PDF
- 技术规范是 Word
- 公告是 Markdown
- API 文档是 OpenAPI JSON

**这些格式怎么读?怎么切?怎么喂给 LLM?**

如果用 Java 标准库:
- 读 PDF:`PDFBox`(纯 Java 库,API 复杂)
- 读 Word:`POI`(Apache POI,代码冗长)
- 读 HTML:`Jsoup`(还行,但要自己清理)
- 切块:自己写 split 算法(token 计数、overlap)

**Spring AI Document Loaders 把这些统一了**:

```java
// 任何格式,一行
TikaDocumentReader reader = new TikaDocumentReader(pdfResource);
List<Document> docs = reader.read();
```

底层用 **Apache Tika**(Java 生态最强的文档解析库),支持 1000+ 种格式。

## 关键概念(4 个)

### 概念 1:`Document`

Spring AI 文档抽象,**一段文本 + metadata**:

```java
public class Document {
    private final String id;                  // 可选,UUID
    private final String text;                 // 内容
    private final Map<String, Object> metadata;  // 元数据
}
```

**Metadata 用来干嘛?**
- 过滤检索:`filterExpression("type == 'policy'")`
- 跟踪来源:`metadata.put("source", "employee-handbook.md")`
- 去重:用 `(text, metadata.source)` 当 docId

### 概念 2:`DocumentReader`

读文件 → `List<Document>`,按文件类型分:

| Reader | 输入 | 输出 | 切分粒度 |
|---|---|---|---|
| `MarkdownDocumentReader` | .md | 多 Document | **按 ## 标题** |
| `TikaDocumentReader` | HTML/PDF/Word/PPT | 1 Document | 整文件 |
| `JsonReader` | .json | 多 Document | 数组元素 |
| 自定义 | 任意 | 任意 | 自己实现 |

**Markdown vs Tika 关键区别**:Markdown 按章节切(语义好),Tika 整文件一个(粗)。

### 概念 3:`DocumentSplitter`

`Document` → 多个 `Document`(按 token 切):

| Splitter | 切分方式 | 适用 |
|---|---|---|
| `TokenTextSplitter` | token 数 | **通用默认** |
| `RecursiveCharacterTextSplitter` | 段落/句子 | 长文本 |
| `SemanticChunker` | 语义相似度 | 高质量但慢 |
| 自定义 | 任意 | 特殊场景 |

**为什么需要切分?**
- LLM 上下文有限(gpt-4o-mini 128K,但实用 4K-16K)
- 一个 5000 行 Markdown 不切,直接喂 LLM 会爆
- 切太小(50 token)→ 检索命中但信息不全
- 切太大(2000 token)→ 超上下文 + 慢

### 概念 4:Chunk Size & Overlap

**黄金参数**:
- **chunk size**:200-500 token(大多数场景)
- **overlap**:50-100 token(防止信息被切断)

```
原始: [1──────500──────1000]
切块: [1──200][180──380][360──560][540──740]...
                                 ↑
                            overlap 50
```

overlap 让相邻 chunk 共享 50 token,**关键句子被切两半时不会丢信息**。

## 关键代码(4 个实战场景)

### 场景 1:读 Markdown(按章节)

```java
MarkdownDocumentReaderConfig mdConfig = MarkdownDocumentReaderConfig.builder()
    .withHorizontalRuleCreateDocument(true)   // --- 分割也算独立 doc
    .withIncludeCodeBlock(false)              // 跳过 ``` 代码块
    .withIncludeBlockquote(false)             // 跳过 > 引用
    .build();

MarkdownDocumentReader mdReader = new MarkdownDocumentReader(handbookResource, mdConfig);
List<Document> mdDocs = mdReader.read();
// employee-handbook.md → N 个 Document(每 ## 章节一个)
```

**`MarkdownDocumentReaderConfig` 3 个开关**:
- `withHorizontalRuleCreateDocument(true)` — `---` 切分
- `withIncludeCodeBlock(false)` — 跳过代码块(代码语义独立,跟正文混一起会乱)
- `withIncludeBlockquote(false)` — 跳过引用(避免重复)

### 场景 2:读 HTML/Tika(PDF/Word/PPT)

```java
// HTML
TikaDocumentReader tikaReader = new TikaDocumentReader(faqResource);
List<Document> htmlDocs = tikaReader.read();

// PDF(同样代码,只要换 resource)
TikaDocumentReader pdfReader = new TikaDocumentReader(pdfResource);

// Word
TikaDocumentReader wordReader = new TikaDocumentReader(docxResource);
```

**Tika 一行解决 1000+ 格式**,但默认是**整文件 1 个 Document**(粗)。要细分,见场景 3。

### 场景 3:TokenTextSplitter 切块

```java
TokenTextSplitter splitter = TokenTextSplitter.builder()
    .withChunkSize(200)              // 每块 200 token
    .withMinChunkSizeChars(50)       // 小于 50 char 丢弃(避免空块)
    .withMinChunkLengthToEmbed(5)    // 至少 5 char 才 embed
    .withMaxNumChunks(10000)         // 安全上限
    .withKeepSeparator(true)         // 保留段落分隔符
    .build();

List<Document> chunks = splitter.apply(allRawDocs);
// 10 个原始 doc → 50 chunks
```

**为什么 Spring AI 推荐 200?**
- 太小(50):信息不全,检索匹配但答非所问
- 太大(2000):超 LLM 上下文,响应慢
- **200-500 是社区经验值**

### 场景 4:端到端 RAG Pipeline

```java
// 1. 读
List<Document> raw = new MarkdownDocumentReader(resource).read();

// 2. 切
List<Document> chunks = new TokenTextSplitter().apply(raw);

// 3. 存
vectorStore.add(chunks);

// 4. 问
ChatClient client = builder
    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    .defaultSystem("你是 HR / IT 助手,基于公司文档回答")
    .build();

String answer = client.prompt().user("试用期多久?").call().content();
```

**4 步流水线**:读 → 切 → 存 → 问。任何 RAG 系统本质都是这 4 步。

## 选型决策树

```
要加载的文档是什么格式?
├── Markdown → MarkdownDocumentReader(按 ## 切)
├── HTML / PDF / Word → TikaDocumentReader(整文件)
├── JSON / JSONL → JsonReader
└── 自定义格式 → 实现 DocumentReader 接口

要不要按章节切?
├── 是(语义优先) → Markdown / 配置 TikaPage
└── 否(粗切后自己分) → 走 TokenTextSplitter

chunk size 怎么选?
├── 短回答(FAQ) → 100-200 token
├── 长文档(手册) → 300-500 token
├── 代码 → 500-1000 token
└── 不确定 → 200 + overlap 50
```

## 4 种 Reader 对比

| Reader | 输入 | 输出 | 切分粒度 | 优缺点 |
|---|---|---|---|---|
| `MarkdownDocumentReader` | .md | 多 Document | **按 ## 标题** | 语义好,但只支持 MD |
| `TikaDocumentReader` | HTML/PDF/Word/PPT | 1 Document | 整文件 | 格式广,但粗 |
| `JsonReader` | .json | 多 Document | 数组元素 | 结构化,但要预处理好 JSON |
| `PagePdfDocumentReader` | .pdf | 多 Document | **每页一个** | PDF 专用,细粒度 |

**PDF 进阶**:`PagePdfDocumentReader` 把每页当一个 Document,比 `TikaDocumentReader` 细。

## 4 种 Splitter 对比

| Splitter | 切分方式 | 速度 | 适用 |
|---|---|---|---|
| `TokenTextSplitter` | token 数 | 快 | **通用默认** |
| `RecursiveCharacterTextSplitter` | 段落/句子 | 快 | 长文本,语义段落切 |
| `SemanticChunker` | 语义相似度 | 慢(要调 LLM) | 高质量 |
| 自定义 | 任意 | 任意 | 特殊场景(代码/表格) |

**`SemanticChunker`** 调 LLM 找语义边界,质量好但慢(每个 chunk 决策都要调 LLM)。生产慎用,成本高。

## 测试(纯本地 0 网络)

ch9 测试策略跟 ch8 一样:`@SpringBootTest` 启动时**不主动调 Reader**(lazy),所以不读文件不连接数据库。

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplicationTests {
    @Test
    void contextLoads() {
        // 0 网络(VectorStore 延迟初始化)
    }
}
```

**要真测 Reader**:加一个测试方法,临时改 active profile 跑 main runner。

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| Markdown 大文件(>5000 行) | 1 个 chunk 超 LLM 上下文 | 强制 TokenTextSplitter 切 |
| Tika 读 PDF 没分页 | 整 PDF 一个 Document | 用 `PagePdfDocumentReader` |
| Splitter 切太碎(50 token) | 检索命中但信息不全 | 调大到 300-500 |
| Splitter 切太大(2000 token) | 超上下文 + 慢 | 调小到 200-500 |
| `withIncludeCodeBlock(true)` | 代码混进正文,污染检索 | 改 `false` |
| 中文 Tika 编码乱 | `???` 乱码 | Spring AI 默认 UTF-8,检查文件编码 |
| PDF 是扫描版(图片) | Tika 读出来是空 | 走 OCR(Tesseract),不属本章范围 |
| overlap 设 0 | 关键句子被切断 | 至少 overlap=50 |

## 实战部署清单

- [ ] 准备 sample-docs/(MD/HTML/PDF/Word)
- [ ] 选 Reader(Markdown / Tika / Json)
- [ ] 选 Splitter(默认 TokenTextSplitter)
- [ ] chunk size 200-500,overlap 50-100
- [ ] 跑 `mvn spring-boot:run` 验证读取
- [ ] 检查 chunks 数量(预期 原始 N → 切后 5-10N)
- [ ] pgvector 存了 chunks
- [ ] retrieve + QuestionAnswerAdvisor 跑通
- [ ] `mvn test` 0 网络 PASS
- [ ] 大文件(>10MB)测一次,看切块耗时

## 完整代码

[02-rag/09-document-loaders/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/09-document-loaders)

## 下一步

- [第 10 章 · Advanced RAG →](10-advanced-rag.md)— Multi-Query 扩展 / Hybrid Search / RetrievalAugmentationAdvisor 组合
- [第 12 章 · RAG Production →](12-rag-production.md)— 增量更新 / 缓存 / 监控
- 切到真 LLM?看 [真实 LLM 接入指南](guides/00-真实LLM接入.md)
