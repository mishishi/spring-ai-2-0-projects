# Project 2: 企业文档问答助手

> Phase 4 实战项目 2/5 · 0 网络可跑(关键词检索 RAG)

## 解决什么问题

企业内部有大量制度文档(员工手册 / FAQ / 规范),员工问"年假怎么休"得翻 30 页 PDF。
本项目把文档加载到内存,用户问问题 → **关键词检索** 找相关段落 → 模板化答案 + 引用。
真实生产换 SimpleVectorStore + EmbeddingModel + ChatClient。

## 架构

```
                    ┌──────────────────┐
                    │  启动时 @PostConstruct
                    │  加载 sample-docs/*.md
                    │  → 按段落切块
                    │  → tokenize (中文 2-gram + 英文)
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
                          "【命中 3 个文档段落】..."
```

## 关键技术

- **中文 2-gram 分词**:解决"单字命中噪音"问题
- **关键词评分**:query 里每个 token 在 doc 里出现次数累加
- **0 网络 mock 模板**:`if (snippet.contains("报销")) → "请参考报销流程"`,规则化兜底
- **真实 LLM 路径**:ChatClient 不为 null 时拼"参考文档 + 问题"给 LLM 答

## 跑起来

```bash
# 0 网络测试
mvn -pl 04-projects/project-2-doc-qa test

# 真实跑(需 OPENAI_API_KEY)
export OPENAI_API_KEY=sk-xxxxx
cd 04-projects/project-2-doc-qa
mvn spring-boot:run

# 调用
curl -X POST http://localhost:8080/qa/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "怎么报销?"}'
```

## 核心代码

- `DocQaService` — 关键词检索 + 模板化回答
- `DocQaController` — POST /qa/ask
- `src/main/resources/sample-docs/handbook.md` — 演示文档

## 学到啥

- Spring AI 2.0 RAG 概念(章节 7-12 基础)
- 关键词检索 vs 向量检索的权衡
- 中文分词(2-gram)的实现思路
- 0 网络 mock 模板设计
- 真实 LLM 路径的 fallback 切换

## 扩展方向

- 换 SimpleVectorStore + EmbeddingModel(章节 7)做语义检索
- 换 PgVector / Qdrant / Elasticsearch(章节 8)做生产存储
- 加 DocumentReader(Markdown / PDF / HTML)— 章节 9
- 加 Rerank(精确度提升)— 章节 11
- 多租户 + 文档权限
