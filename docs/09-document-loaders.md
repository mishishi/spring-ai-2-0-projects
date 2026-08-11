# 第 9 章 · Document Loaders


## 一句话总结

`DocumentReader` 读文件(自动按格式切)→ `DocumentSplitter` 按 token 切块 → `vectorStore.add()` 持久化。Spring AI 一行配置搞定。

## 读者学完能做什么

- 加载 MD / HTML / PDF / Word
- 按 token 切块
- 完整 RAG pipeline
- 选择合适的 Reader / Splitter

## 完整代码

[02-rag/09-document-loaders/](https://github.com/mishishi/spring-ai-2-0-projects/tree/main/02-rag/09-document-loaders)

## 3 种 Reader

| Reader | 输入 | 输出 |
|---|---|---|
| MarkdownDocumentReader | .md | 按 ## 切多 Document |
| TikaDocumentReader | HTML/PDF/Word | 整文件 1 Document |
| JsonReader | .json | 数组元素 |

## 4 种 Splitter

| Splitter | 切分方式 |
|---|---|
| TokenTextSplitter | token 数(默认) |
| RecursiveCharacterTextSplitter | 段落/句子 |
| SemanticChunker | 语义 |
| 自定义 | 任意 |

## 踩坑预警

| 坑 | 现象 | 解决 |
|---|---|---|
| Markdown 大文件(>5000 行) | 1 个 chunk 超 LLM 上下文 | 强制 TokenTextSplitter 切 |
| Tika 读 PDF 没分页 | 整 PDF 一个 Document | 用 PagePdfDocumentReader |
| Splitter 切太碎(50 token) | 检索命中但信息不全 | 调大到 300-500 |
| Splitter 切太大(2000 token) | 超上下文 + 慢 | 调小到 200-500 |

## 下一步

- [第 10 章 · Advanced RAG →](10-advanced-rag.md)
- Multi-Query 扩展 / Hybrid Search / RetrievalAugmentationAdvisor 组合
