# 职跃 —— 校园求职训练与面试能力成长平台

面向高校就业指导场景的求职训练与面试评估系统。围绕 **岗位能力要求 → 简历准备 → 模拟面试训练 → 证据化评价 → 薄弱项分析 → 成长计划 → 复练跟踪** 建立完整业务闭环，为求职者、面试官（就业指导教师）和平台管理员提供角色化的工作台与数据视图。

## 技术栈

### 后端（app/）

| 技术 | 版本 | 说明 |
| --- | --- | --- |
| Java | 21 | 开发语言（虚拟线程） |
| Spring Boot | 4.1.0 | 应用框架 |
| Gradle | 8.14 | 构建工具 |
| PostgreSQL | 14+（自带 pgvector） | 关系数据库 + 向量存储 |
| Redis | 6+ | 缓存 + 消息队列（Stream 异步任务） |
| MinIO | - | S3 兼容对象存储（简历/知识库文件） |
| Apache Tika | 2.9.2 | 文档解析 |
| iText | 8 | PDF 导出 |
| MapStruct | 1.6.3 | 对象映射 |
| SpringDoc OpenAPI | 3.0.2 | 接口文档 |

大语言模型接入采用业界通用的大模型接口协议，支持多服务商配置（DashScope、Kimi、DeepSeek、GLM、LM Studio 等），并提供确定性本地演示模式（无需任何密钥即可完整演示业务闭环）。

### 前端（frontend/）

| 技术 | 版本 |
| --- | --- |
| React | 18.3 |
| TypeScript | 5.6 |
| Vite | 5.4 |
| Tailwind CSS | 4.1 |
| React Router | 7 |
| Recharts | 3.6 |
| pnpm | 10+ |

## 功能概览

- **三角色 RBAC**：求职者 / 面试官（就业指导教师）/ 管理员，独立登录入口与角色工作台，权限与数据归属由后端统一裁定。
- **求职者**：简历上传与智能分析、岗位探索、训练计划、文字/语音模拟面试、证据化评估报告、成长档案与趋势。
- **面试官**：岗位能力模型创建与发布、候选人面试记录查看、面试日程管理。
- **管理员**：用户与角色管理、岗位审核、知识库管理、模型服务配置（密钥加密存储）、审计日志。
- **评价证据化**：每道题的评分附带回答原文证据句，无证据内容显示"信息不足"，不编造评价依据。
- **异步任务**：简历分析、面试评估、知识库向量化基于 Redis Stream 异步处理，支持失败重试。

## 环境要求

| 依赖 | 版本 | 必需 | 说明 |
| --- | --- | --- | --- |
| JDK | 21+ | 是 | 后端运行 |
| Node.js | 18+ | 是 | 前端构建 |
| pnpm | 10+ | 推荐 | 前端包管理器 |
| PostgreSQL | 14+ | 是 | 需启用 pgvector 扩展 |
| Redis | 6+ | 是 | 缓存与消息队列 |
| MinIO（或其他 S3 兼容存储） | - | 是 | 文件存储 |

## 快速启动

### 1. 配置环境变量

复制 `.env.example` 为 `.env`（位于项目根目录，后端启动时自动读取）：

```bash
cp .env.example .env
```

主要配置项：

```bash
# 数据库（与本地 PostgreSQL 一致）
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/interview_guide
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=你的数据库密码

# 对象存储（与本地 MinIO 一致）
APP_STORAGE_ENDPOINT=http://localhost:9000
APP_STORAGE_ACCESS_KEY=minioadmin
APP_STORAGE_SECRET_KEY=minioadmin

# 模型服务配置加密密钥（任意足够长的随机串，必填）
APP_AI_CONFIG_ENCRYPTION_KEY=change_me_provider_api_key_encryption

# 本地演示模式（无模型密钥时可完整演示，默认 true；接入真实模型后可关闭）
APP_DEMO_ENABLED=true
```

如需接入真实大语言模型服务，在 `.env` 中补充对应密钥（如 `AI_BAILIAN_API_KEY`），或在管理员"模型服务设置"页面配置。

### 2. 启动依赖服务

**方式一：Docker（推荐）**

```bash
docker compose -f docker-compose.dev.yml up -d
```

一键启动 PostgreSQL（pgvector）、Redis、MinIO 三个依赖。

**方式二：本地安装**

自行安装并启动 PostgreSQL（启用 pgvector 扩展）、Redis、MinIO，确保端口与 `.env` 一致（默认 5432/6379/9000）。

### 3. 初始化数据库

创建数据库（首次启动时建表由 JPA 自动完成，`ddl-auto=update`）：

```sql
CREATE DATABASE interview_guide;
```

pgvector 扩展随库初始化自动创建。

### 4. 启动后端

```bash
./gradlew :app:bootRun
```

后端启动于 **http://localhost:8080**（纯 REST API，接口文档见 http://localhost:8080/swagger-ui/index.html ）。

本地开发使用 `local` profile（演示账号种子等开发配置）：

```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE='local'; .\gradlew.bat :app:bootRun
```

### 5. 启动前端

```bash
cd frontend
corepack enable
pnpm install
pnpm dev
```

前端启动于 **http://localhost:5173**（页面入口，`/api` 自动代理到后端 8080）。

### 6. 访问系统

打开 http://localhost:5173 ，选择身份入口登录。

演示账号（仅 `local` profile 自动创建）：

| 角色 | 入口 | 账号 | 密码 |
| --- | --- | --- | --- |
| 求职者 | /login/candidate | candidate | candidate123 |
| 面试官 | /login/interviewer | interviewer | interviewer123 |
| 管理员 | /login/admin | admin | admin123 |

## 构建与测试

```bash
# 后端编译
./gradlew :app:compileJava

# 后端全量测试
./gradlew :app:test --no-daemon

# 后端可执行包
./gradlew :app:bootJar

# 前端类型检查与生产构建
cd frontend && pnpm run build
```

## 项目结构

```
├── app/                              # 后端应用（Spring Boot）
│   └── src/main/java/interview/guide/
│       ├── common/                   # 通用能力（模型服务接入、限流、异步模板、异常、统一响应）
│       ├── infrastructure/           # 基础设施（文件、导出、Redis、MapStruct）
│       ├── auth/                     # 认证与 RBAC（JWT、用户、审计）
│       └── modules/                  # 业务模块
│           ├── growth/               # 岗位 / 训练计划 / 成长记录
│           ├── interview/            # 模拟面试（出题、作答、评估）
│           ├── resume/               # 简历（上传、分析、导出）
│           ├── interviewschedule/    # 面试安排
│           ├── knowledgebase/        # 知识库与检索问答
│           ├── llmprovider/          # 多模型服务配置
│           └── voiceinterview/       # 语音面试
├── frontend/                         # 前端应用（React + TypeScript + Vite）
│   └── src/
│       ├── api/                      # API 客户端
│       ├── components/               # 公共组件
│       ├── context/                  # 登录态上下文
│       ├── pages/                    # 页面组件
│       └── constants/                # 路由常量
├── docs/                             # 设计与开发文档
├── docker-compose.dev.yml            # 本地开发依赖（PG/Redis/MinIO）
└── .env.example                      # 环境变量示例
```

## 常见问题

**Q：后端 8080 打开后没有页面？**
后端是纯 REST API 服务（前后端分离），页面入口是前端 http://localhost:5173 ；接口文档在 http://localhost:8080/swagger-ui/index.html 。

**Q：简历分析一直"分析中"？**
检查 Redis 是否正常连接（异步分析依赖 Redis Stream），并查看后端日志。

**Q：没有模型服务密钥能演示吗？**
可以。`.env` 中 `APP_DEMO_ENABLED=true`（默认）时，出题、评估、简历分析均使用确定性本地逻辑，全程标注"本地演示模式"，可完整演示业务闭环。

**Q：知识库向量化失败？**
确认 PostgreSQL 已启用 pgvector 扩展，且已配置可用的向量模型服务。

**Q：Windows PowerShell 下后端日志中文乱码？**
在启动前执行：

```powershell
chcp 65001 | Out-Null
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
```
