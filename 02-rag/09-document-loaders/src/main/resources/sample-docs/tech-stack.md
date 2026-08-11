# 技术栈规范

## 后端

- Java 17 + Spring Boot 4.0
- 数据库:PostgreSQL 16
- ORM:Spring Data JPA
- Cache:Redis 7
- 消息队列:RabbitMQ
- 搜索引擎:Elasticsearch 8

## 前端

- React 18 + TypeScript
- 状态管理:Zustand(替代 Redux)
- UI:shadcn/ui + Tailwind CSS
- 构建:Vite
- 测试:Vitest + Playwright

## 部署

- 容器:Docker
- 编排:Kubernetes
- CI/CD:GitHub Actions
- 监控:Prometheus + Grafana
- 日志:Loki
- 链路追踪:OpenTelemetry

## AI / ML

- 框架:Spring AI 2.0
- LLM:OpenAI GPT-4o / DeepSeek
- Embedding:text-embedding-3-small
- Vector Store:pgvector
- RAG:QuestionAnswerAdvisor + 多 query 扩展
- Agent:@Tool + 多 Agent 协作

## 代码规范

- Java:Google Java Style + Spotless
- TypeScript:ESLint + Prettier
- 提交规范:Conventional Commits
- PR 流程:必须 1 个 review + CI 全过
