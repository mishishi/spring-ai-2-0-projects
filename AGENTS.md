# AGENTS.md

> AI 智能体协作指南(per [agents.md spec](https://agents.md/))
> 消费方:OpenCode / Codex / Cursor / Aider / Devin / Gemini CLI / Claude Code / Mavis

## 项目概述

**spring-ai-2-0-projects** — Spring AI 2.0 项目实战重做版

从零到深度,4 个 phase + 5 个完整可部署项目。面向有 Java 基础、会 Spring Boot 的工程师。

这是 v2 重做版(前身 `spring-ai-2-0-in-action` v1 已在 2026-08-11 归档)。

## 技术栈(P0 拍板,2026-08-11)

| 维度 | 选择 | 理由 |
|---|---|---|
| Java | 17 | LTS + 生态成熟 |
| Spring Boot | 4.1.0 | 最新稳定 |
| Spring AI | 2.0.0(BOM) | 跟最新版本 |
| 构建 | Maven(monorepo 多模块) | Java 工程师熟悉 |
| 测试 | JUnit 5 + Mockito + AssertJ | 纯本地,0 网络 |
| 代码风格 | Google Java Style | |
| 站点 | mkdocs-material(后续) | 跟 v1 风统一 |

## 目录结构(目标态)

```
spring-ai-2-0-projects/
├── 01-basics/              # Phase 1: 6 章基础
│   ├── 01-hello-world/
│   ├── 02-chatclient-api/
│   ├── 03-prompt-advisor/
│   ├── 04-function-calling/
│   ├── 05-structured-output/
│   └── 06-streaming/
├── 02-rag/                 # Phase 2: 6 章 RAG
│   ├── 07-rag-overview/
│   ├── 08-pgvector/
│   ├── 09-document-loaders/
│   ├── 10-advanced-rag/
│   ├── 11-reranking/
│   └── 12-rag-production/
├── 03-agent/               # Phase 3: 6 章 Agent
│   ├── 13-agent-basics/
│   ├── 14-tool-calling/
│   ├── 15-mcp/
│   ├── 16-multi-agent/
│   ├── 17-spring-ai-graph/
│   └── 18-agent-production/
├── 04-projects/            # 5 个完整可部署项目
│   ├── project-1-weekly-report/    # AI 周报生成器
│   ├── project-2-doc-qa/           # 企业文档问答助手
│   ├── project-3-travel-planner/   # AI 旅行规划助手
│   ├── project-4-code-review/      # AI Code Review 助手
│   └── project-5-knowledge-hub/    # 综合知识库(可选)
├── docs/                   # mkdocs 站点源
│   ├── index.md
│   ├── 00-reading-guide.md
│   └── decisions/          # 决策记录
├── AGENTS.md               # 本文件
├── README.md
├── LICENSE                 # MIT
└── pom.xml                 # 根 Maven POM(聚合模块)
```

## 常用命令

```bash
# 编译所有模块
mvn clean compile

# 测试(纯本地,0 网络)
mvn test

# 跑单个 module
mvn -pl 01-basics/01-hello-world spring-boot:run
mvn -pl 01-basics/01-hello-world test

# 打包
mvn -DskipTests package
```

## ⚠️ P0 事故教训(2026-08-11 v1 事故)

上次 30 天书项目出过严重 P0 事故,**这次必须遵守的规则**:

1. **第一个 commit 就 push**:`git init` → `git remote add origin` → `git push -u origin main`,绝不只本地
2. **每个 chapter 完成立即 push**:不要"攒一波"
3. **大操作前 bundle**:`git bundle create backup-$(date +%Y%m%d).bundle --all && cp ~/Documents/`
4. **Java 源码和站点分离仓**:站点仓放 mkdocs 输出,Java 源码单独仓
5. **mavis-trash 不在大目录用**:可能走 osascript Finder delete 走特殊机制,难恢复
6. **看到 .git 没了不要直接 git init**:先 `git reflog / fsck` 抢救
7. **每个 phase 完成,执行 backup-checklist**:
   - [ ] git status clean
   - [ ] git push origin main
   - [ ] git bundle create backup.bundle --all
   - [ ] cp backup.bundle ~/Documents/
8. **GitHub remote 必须用真实存在的账号**:`ginko-friday/...` 不存在,要用 `mishishi/...`

## 决策记录(Decision Log)

完整决策见 [`docs/decisions/`](docs/decisions/) 目录。摘要:

| 日期 | 决策 ID | 内容 |
|---|---|---|
| 2026-08-11 | D-1 | 问题诊断 = B(内容质量)+ C(项目实战)+ D(跟 API 版本) |
| 2026-08-11 | D-2 | 老 Vercel 项目删除(已执行) |
| 2026-08-11 | D-3 | 老本地仓库保留只读,放在 `~/.mavis/agents/mavis/workspace/` |
| 2026-08-11 | D-4 | 新仓库 = `mishishi/spring-ai-2-0-projects`(修正自 `ginko-friday`) |
| 2026-08-11 | D-5 | 技术栈 = Java 17 + Spring Boot 4.1.0 + Spring AI 2.0.0 |

## 5 个实战项目(2026-08-12 完成)

| 阶段 | 项目 | 重点技术 | 状态 |
|---|---|---|---|
| 4-P1 | **AI 周报生成器** | ChatClient + PromptTemplate + 0 网络 mock | ✅ |
| 4-P2 | **企业文档问答助手** | 关键词检索 RAG + ChatClient 真 LLM 路径 | ✅ |
| 4-P3 | **AI 旅行规划师** | 4 sub-agent + 1 orchestrator (Multi-Agent) | ✅ |
| 4-P4 | **AI 代码审查器** | @Tool 静态分析 + ChatClient 语义审查 | ✅ |
| 4-P5 | **AI 综合知识中心** | QueryRouter 路由 + RAG + Tool + Memory 整合 | ✅ |

> **23 module 全部完成**:Phase 1 (6) + Phase 2 (6) + Phase 3 (6) + Phase 4 (5) = 23 个独立 Maven 模块,所有 `mvn test` 0 网络全绿。

## 阶段路线(20 周)

W0 基建 → W1-4 Phase 1 → W5-10 Phase 2 → W11-16 Phase 3 → W17-20 综合

## 跟 v1(30 天书)的差异

| 维度 | v1 `spring-ai-2-0-in-action` | v2 `spring-ai-2-0-projects` |
|---|---|---|
| 定位 | 概览覆盖(breadth) | 项目实战(depth + breadth) |
| 项目深度 | Demo 级 | 完整可上线 |
| 实战数量 | 3 个 + 65 tests | 5 个 + 100+ tests(目标) |
| 节奏 | 一次性 | 20 周渐进 |
| 状态 | 已归档 | 进行中 |
