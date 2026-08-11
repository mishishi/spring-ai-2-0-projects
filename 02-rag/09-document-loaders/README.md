# 第 9 章 · Document Loaders

> 🎯 目标:加载真实文档(Markdown / HTML / PDF / Word)+ 切块 + 存到 vector store

## 你将学到

- ✅ `MarkdownDocumentReader`(按 `##` 切分)
- ✅ `TikaDocumentReader`(读 HTML / PDF / Word / PPT)
- ✅ `TokenTextSplitter`(按 token 切块,避免超 LLM 上下文)
- ✅ 完整 pipeline:Load → Split → Embed → Store

## 快速开始

```bash
cd 02-rag/09-document-loaders
export OPENAI_API_KEY=sk-xxxxx
mvn spring-boot:run
```

6 个 demo(用 `══════` 分隔):
- 读 `employee-handbook.md` → 4 个章节
- 读 `faq.html` → 1 个 Document(整个 HTML)
- 按 token 切块 → 多个 chunks
- 存到 pgvector
- 4 个问题 RAG 问答
- 手动 retrieve 看 chunk 详情

## 关键代码

### 1. Markdown 读取(按 ## 章节切)

```java
MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
    .withHorizontalRuleCreateNewDocument(true)
    .withIncludeCodeBlock(false)
    .withIncludeBlockquote(false)
    .build();
MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
List<Document> docs = reader.read();
// 每个 ## 章节一个 Document
```

### 2. Tika 读 HTML/PDF/Word

```java
TikaDocumentReader reader = new TikaDocumentReader(resource);
List<Document> docs = reader.read();
// 整个文件一个 Document
```

**Tika 支持的格式**:HTML / PDF / Word / Excel / PPT / RTF / OpenOffice / ...

### 3. TokenTextSplitter 切块

```java
TokenTextSplitter splitter = new TokenTextSplitter(
    200,    // default chunk size(tokens)
    50,     // min chunk size
    5,      // min chunk length to embed
    10000,  // max num chunks
    true    // keep separator
);
List<Document> chunks = splitter.apply(documents);
```

**为什么按 token 切**?LLM 上下文有 token 上限,gpt-4o-mini 128K,但单次 RAG 取 top-k docs 总长要可控。

### 4. 完整 pipeline

```java
List<Document> raw = markdownReader.read();      // Load
List<Document> chunks = splitter.apply(raw);    // Split
vectorStore.add(chunks);                         // Embed + Store
```

## 3 种 Reader 对比

| Reader | 格式 | 切分方式 |
|---|---|---|
| `MarkdownDocumentReader` | .md | 按 `##` 标题切 |
| `TikaDocumentReader` | HTML / PDF / Word | 整文件 1 个 Document |
| `JsonReader` | .json | 数组元素 |

**`MarkdownDocumentReader` 是最结构化的**(按章节切,自动生成 metadata)。

## 4 种 Splitter 对比

| Splitter | 切分依据 | 何时用 |
|---|---|---|
| `TokenTextSplitter` | token 数 | **默认推荐**,LLM 友好 |
| `RecursiveCharacterTextSplitter` | 段落 / 句子 | 通用 |
| `SemanticChunker` | 语义相似度 | 高级(慢) |
| 自定义正则 | 任意 | 特殊需求 |

## 实战模式

| 场景 | 文档 | Reader | Splitter |
|---|---|---|---|
| 公司文档 | PDF | `PagePdfDocumentReader` | `TokenTextSplitter(500)` |
| 技术博客 | MD | `MarkdownDocumentReader` | 按 ## 切(自动) |
| FAQ | HTML | `TikaDocumentReader` | `TokenTextSplitter(200)` |
| 代码 | .java | 自定义(按 class / method 切) | - |

## 测试

```bash
mvn test
```

0 网络(VectorStore 延迟初始化,contextLoads 不调 OpenAI)。

## 目录结构

```
09-document-loaders/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/cc/misshi/springai/docsloader/
    │   │   └── Application.java
    │   └── resources/
    │       ├── application.yml
    │       └── sample-docs/
    │           ├── employee-handbook.md  (4 章节)
    │           ├── tech-stack.md
    │           └── faq.html
    └── test/
        └── java/cc/misshi/springai/docsloader/
            └── ApplicationTests.java
```

## 下一章

[第 10 章 · Advanced RAG →](../10-advanced-rag/README.md)

Multi-Query 扩展 / Hybrid Search / Spring AI 2.0 RetrievalAugmentationAdvisor
