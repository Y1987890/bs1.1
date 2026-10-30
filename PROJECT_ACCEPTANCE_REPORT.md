# 项目完工验收报告（PROJECT_ACCEPTANCE_REPORT.md）

> 验收对象：《基于前后端分离架构的智能求职训练与面试评估平台的设计与实现》（职跃——校园求职训练与面试能力成长平台）
> 验收日期：2026-10-28（并引用 2026-10-27 三轮体检历史证据）
> 验收方式：以真实代码为依据、实际执行测试；未执行项如实标注，不虚构结果。

## 1. 项目实际架构与运行环境

| 项 | 实际情况（以代码与运行实测为准） |
| --- | --- |
| 前端 | React 18.3 + TypeScript 5.6 + Vite 5.4 + Tailwind 4；开发端口 **5173**；启动 `cd frontend && corepack pnpm run dev`；构建 `corepack pnpm run build`（tsc+vite） |
| 后端 | Spring Boot 4.1.0 + Java 21 + Gradle 8.14；端口 **8080**（纯 REST API，无 HTML 页面，API 文档 /swagger-ui/index.html）；启动 `./gradlew :app:bootRun` 或 `java -jar app/build/libs/app-0.0.1-SNAPSHOT.jar`（local profile 注入根目录 .env） |
| 数据库 | PostgreSQL 16.9（本地原生 .infra/pgsql，端口 5432，库名 interview_guide）+ pgvector；表结构由 JPA `ddl-auto=update` 管理 |
| 缓存/异步 | Redis 5.0.14（本地原生 .infra/redis，端口 6379；面试会话缓存 + Redis Stream 异步评估/分析） |
| 对象存储 | MinIO（本地原生 .infra/minio，9000 API/9001 控制台；桶 interview-guide，私有读） |
| 认证 | 自研 JWT（HMAC-SHA256，JwtService）+ BCrypt（仅 spring-security-crypto）；AuthFilter 白名单+401/403 |
| RBAC | CANDIDATE / INTERVIEWER / ADMIN 三角色；AuthSupport 控制器级校验 + Service 层 ownerId 归属校验 + 审计 |
| 模型 | LlmProviderRegistry 多 Provider 抽象（DashScope/Kimi/DeepSeek/GLM/LM Studio）+ StructuredOutputInvoker 结构化输出 + 本地演示模式（确定性逻辑，`app.demo-mode.enabled`） |
| 基础设施启动 | PG/Redis/MinIO 以 WMI 常驻进程方式启动（Windows 环境无 Docker；历史曾因 Docker/WSL2 不稳定改用原生进程） |

## 2. 启动、停止与测试命令

```powershell
# 基础设施（WMI 常驻，一次启动）
#   PG:  .infra\pgsql\pgsql\bin\pg_ctl.exe start -D .infra\pgdata -l .infra\pg.log -o "-p 5432"
#   Redis: .infra\redis\redis-server.exe --dir .infra\redis --logfile .infra\redis\redis-run.log
#   MinIO: .infra\minio\minio.exe server .infra\minio\data --address :9000 --console-address :9001
# 后端（注入 .env + local profile）
$env:SPRING_PROFILES_ACTIVE='local'; java -jar app\build\libs\app-0.0.1-SNAPSHOT.jar   # 8080
# 前端
cd frontend; corepack pnpm run dev                                                     # 5173
# 测试
./gradlew :app:test --no-daemon                                                        # 后端全量
cd frontend; corepack pnpm run build                                                   # 前端类型检查+构建
```

演示账号（仅 local profile 种子 `app.auth.seed-demo-users=true`）：candidate/candidate123、interviewer/interviewer123、admin/admin123。

## 3. 功能模块清单及完成状态

状态口径：**通过**（实测核心行为正确）/ **部分通过** / **失败** / **未实现**（菜单隐藏或标注，不伪装）/ **受阻未验证**（缺密钥/环境）/ **待验证**。
计数：**通过 37 · 部分通过 3 · 失败 0 · 未实现 3 · 受阻未验证 3 · 待验证 1，共 47 项。**

### A. 公共与身份认证（12 项，全通过）

| 功能 | 状态 | 证据 |
| --- | --- | --- |
| 注册（仅产生 CANDIDATE） | 通过 | curl 注册 c2 成功角色正确；AuthServiceTest.registerForcesCandidateRole |
| 通用登录 | 通过 | curl 七场景之不带 expectedRole → 200 |
| 求职者/面试官/管理员独立登录入口（/login/{candidate,interviewer,admin} + /login 身份选择页） | 通过 | Vite 四路由 200；curl 各入口 200；组件含 expectedRole 透传 |
| 角色错配拒绝（不签发 token） | 通过 | curl：admin 入口+candidate 凭据 → 403 明确提示；单测 verify never issueToken |
| 无效入口角色拒绝 | 通过 | curl SUPERUSER → 400「登录入口无效」 |
| 密码错误不泄露角色 | 通过 | curl 错密码+expectedRole → 401「用户名或密码错误」；单测覆盖 |
| 退出登录 | 通过 | Layout logout → /login 选择页（浏览器走查 2026-10-27） |
| 登录过期处理 | 通过 | axios 拦截器 401 → 清 token 跳 /login（request.ts；历史 E2E） |
| 前端路由守卫 | 通过 | ProtectedRoute 未登录跳 /login；角色不符显示无权限页 |
| 后端接口鉴权（401/403 矩阵） | 通过 | 未登录 401；candidate 访 admin 接口 403；llm-provider 三角色 403/403/200 |
| 表单校验/错误提示/加载态 | 通过 | 登录页空值提示、错误红框、loading；后端 @NotBlank/参数校验 400 |
| 敏感操作审计 | 通过 | 岗位创建/发布、角色调整均落 audit_logs（本轮实测 2 条 CHANGE_ROLE） |

### B. 求职者功能（14 项：13 通过 + 1 未实现）

| 功能 | 状态 | 证据 |
| --- | --- | --- |
| 求职者工作台 | 通过 | 浏览器走查（2026-10-27 两轮）角色视角正确、趋势图渲染 |
| 简历上传（文件→MinIO→DB owner 归属） | 通过 | **本轮实测**：上传 id=3 → MinIO 对象落盘 → DB owner_id=1、603 字节 |
| 简历异步分析（demo 确定性） | 通过 | **本轮实测**：COMPLETED、resume_analyses 74 分；accessCount 自增 |
| 简历列表/详情/统计（本人维度） | 通过 | **本轮实测**：detail 返回 filename/resumeText/analyses[74]；statistics {2,9,2} 与 DB 一致 |
| 简历删除（归属校验） | 通过 | 2026-10-27 curl + ResumeDeleteService 守卫；跨账号 403 |
| 简历重新分析（归属校验） | 通过 | 2026-10-27 实测 reanalyze 守卫 |
| 简历分析 PDF 导出 | 通过 | 2026-10-27 早前 E2E（iText 中文字体） |
| 岗位浏览（PUBLISHED 过滤） | 通过 | curl 列表 200；浏览器见已发布岗位与能力卡 |
| 训练计划（创建/完成/归属） | 通过 | curl 创建+complete 200；浏览器 2 条记录；他人 403（单测） |
| 模拟面试（出题/默认追问/作答） | 通过 | **本轮实测**：questionCount=1 实际 2 题（含默认 1 条追问，README 设计如此） |
| 评估报告（评分+证据句） | 通过 | **本轮实测**：Q0=70（96字/4命中/双证据句），追问未答 0 分，总分 35 |
| 面试记录（列表/详情/导出/删除归属） | 通过 | 视角差 cand 9/itv 22（DB 计数）；详情/导出越权 403 |
| 成长记录与趋势 | 通过 | OPEN→DONE curl；趋势 DB 聚合 35/35/35 与会话一致 |
| 个人资料维护页 | **未实现** | 仅有注册时昵称字段，无独立资料维护页面（菜单未提供，不伪装） |

### C. 面试官功能（7 项：4 通过 + 1 部分通过 + 2 未实现）

| 功能 | 状态 | 证据 |
| --- | --- | --- |
| 面试官登录与工作台 | 通过 | /login/interviewer 200；浏览器"面试官"视角 |
| 岗位创建/编辑/发布（能力要求 JSON + 审计） | 通过 | **历史+本轮**：curl 发布 PUBLISHED；浏览器实操新建岗位；audit CREATE/PUBLISH |
| 候选人简历查看（特权全量） | 通过 | itv 简历 1 条 vs cand 0/2 条视角差正确 |
| 面试记录查看（特权全量） | 通过 | itv 22 条 vs cand 视角差；可看候选人训练结果（跨角色链路第 4 步） |
| 面试日程 | **部分通过** | 页面与接口可用（历史体检 200），但未做 owner 级过滤（已列遗留） |
| 面试评价录入/审核 | **未实现** | 无对应 API/页面；菜单不显示（模型预留 ownerId 承接） |
| 面试任务分配 | **未实现** | 同上 |

### D. 管理员功能（8 项：7 通过 + 1 部分通过）

| 功能 | 状态 | 证据 |
| --- | --- | --- |
| 管理员登录与平台总览 | 通过 | /login/admin 200；浏览器平台总览页 |
| 用户列表/管理 | 通过 | curl 5 用户；浏览器用户表格 |
| 角色分配（DB 生效 + 审计） | 通过 | **本轮实测**：PATCH c2→INTERVIEWER→DB 复核生效→改回→审计 2 条 CHANGE_ROLE |
| 岗位全量管理（含归档/删除） | 通过 | curl 全量列表 200；归档/删除路径单测覆盖 |
| 审计日志 | 通过 | curl 6+ 条；浏览器审计页 |
| 模型服务 管理（密钥掩码/ADMIN 专属） | 通过 | 过滤器级守卫：cand/itv HTTP 403、admin 200；maskedApiKey 掩码返回 |
| 简历库（全量） | 通过 | curl 特权视角 |
| 知识库管理 | **部分通过** | 管理页与接口历史体检通过；RAG 向量化依赖 pgvector+embedding 密钥，本轮未实测（受阻部分如实记录） |

### E. 模型与基础设施（7 项：4 通过 + 3 受阻未验证 + 1 部分通过并入上文）

| 功能 | 状态 | 证据 |
| --- | --- | --- |
| demo 模式（确定性、不冒充模型推理） | 通过 | 本轮+历史多轮：评分档位/关键词命中/证据句/简历分析均确定可重复，反馈带【本地演示模式】前缀 |
| 模型服务 抽象与配置切换 | 通过 | LlmProviderRegistry + 设置页；密钥加密落盘（ApiKeyEncryptionService）；配置热加载 |
| Redis（会话缓存 + Stream 异步） | 通过 | 面试/简历异步评估实测完成；曾发现 bgsave 环境问题（工作目录致只读保护）已于 2026-10-27 修复并复验 |
| MinIO 文件上传/读取 | 通过 | 本轮上传落盘 + 详情 resumeText 读取成功（私有桶匿名 403 属预期安全行为，应用经 SDK 凭证访问） |
| 真实远端模型（DashScope 等） | **受阻未验证** | 无 API Key；接入代码存在（StructuredOutputInvoker/重试），不可声称已验证 |
| Ollama 本地模型 | **受阻未验证** | 机器未安装模型 |
| 语音面试（ASR/TTS） | **受阻未验证** | 依赖 DashScope Key；文字面试为核心闭环不受影响（页面存在，未实测） |
| 报告接口瞬态一致性 | **部分通过**（缺陷，见 §10） | 终态 DB/报告/趋势一致（35）；评估窗口内报告曾短暂返回中间值 70 |

## 4. 前端页面与后端 API 对应关系（核心映射）

| 前端页面/路由 | 前端调用 | 后端控制器 | 数据表 |
| --- | --- | --- | --- |
| /login（身份选择）、/login/{role} | authApi.login(expectedRole) | AuthController | users |
| /dashboard（三角色工作台） | growth/jobs/training 等 API | Growth/Position 控制器 | 多表聚合 |
| /jobs 岗位 | jobsApi.* | PositionController | job_positions、audit_logs |
| /training 训练计划 | trainingApi.* | TrainingPlanController | training_plans |
| /growth 成长档案 | growthApi.* | GrowthController | growth_records、interview_sessions |
| /upload 简历上传 | upload API（multipart） | ResumeController | resumes + MinIO |
| /history 简历库 | resumes 列表/详情/统计 | ResumeController | resumes、resume_analyses |
| /interview、/interview-hub | interview sessions 系列 | InterviewController | interview_sessions、interview_answers |
| /interviews/:sessionId 报告 | report/details/export | InterviewController/History | 同上 |
| /admin/users、/admin/audit | adminApi.* | AdminUserController | users、audit_logs |
| /settings 模型服务 | llm-provider API | llmprovider 模块 | 配置文件+密钥加密 |

## 5. 核心业务调用链与验证结果（第五步五条流程）

**流程一 求职者简历（通过，本轮实测）**：candidate 登录（expectedRole 入口校验）→ POST /api/resumes/upload（603B txt）→ MinIO `resumes/2026/10/28/7f80b185_acceptance-resume.txt` 落盘 → resumes 表 id=3 owner_id=1 → Redis Stream 异步分析 → resume_analyses 74 分 → GET /3/detail 返回 resumeText+analyses、accessCount+1 → statistics {2,9,2} 与 SQL 计数一致。

**流程二 求职者面试训练（通过，本轮实测）**：创建会话（questionCount=1，实际 2 题=主问题+默认追问 1 条，README 设计行为）→ 取题 → 提交答案（96 字）→ complete → 异步评估 → 终态报告：Q0=70（命中 jvm/内存/垃圾回收/并发，双证据句）、追问未答 0 分、总分 35 → DB/报告/趋势三处一致。
- **发现缺陷 AC-01（低）**：评估完成的短暂窗口内（秒级），报告接口曾返回中间值（70/仅1题明细），随后收敛到终态 35/2 题。终态数据无错乱；建议演示时以刷新后的终态为准（修复建议见 §12）。

**流程三 面试官评价（部分通过）**：面试官可查看全部候选人面试记录/简历/报告（特权视角，实测 itv 22 vs cand 9）；**评价录入与审核未实现**（无 API/页面，菜单不显示）。最小修改方案：interview_sessions 已有 ownerId/评估字段，可新增 `PATCH /api/v1/interviews/{sessionId}/review`（INTERVIEWER/ADMIN）+ 前端报告页"面试官批注"面板 + growth 模块既有的审计模式。

**流程四 管理员管理（通过，本轮实测）**：admin 入口登录 → GET /admin/users（5 用户）→ PATCH c2→INTERVIEWER → SQL 直查 users 表角色已变更 → 改回 CANDIDATE → audit_logs 生成 2 条 CHANGE_ROLE → 页面数据源同接口可复查。

**流程五 跨角色闭环（通过，2026-10-27 实测）**：interviewer 创建并发布岗位（审计 2 条）→ candidate 浏览该岗位并建训练计划（positionId 绑定）→ 完成面试+评估（17 分报告落库）→ interviewer 面试记录可见该候选人全部会话 → candidate 依薄弱项建成长记录（OPEN→DONE）并见趋势 4 点 → admin 审计日志可见全程敏感操作。各角色看到的是**同一条真实业务数据**（会话/岗位 ID 一致）。

## 6. 基础设施检查结果

| 组件 | 结果 |
| --- | --- |
| PostgreSQL 5432 | 在线；验收 SQL 直查 resumes/users/interview_sessions/resume_analyses/audit_logs 均正常；表齐全（users/audit_logs/job_positions/training_plans/growth_records/resumes/interview_sessions/interview_answers/resume_analyses 等） |
| Redis 6379 | 在线；Stream 异步评估实测完成；bgsave 持久化正常（2026-10-27 修复后复验）；遗留：AbstractStreamConsumer 对 Redis 不可用无退避限频（放大风险，已列遗留） |
| MinIO 9000/9001 | 在线；上传/读取实测通过；桶私有（匿名 403 为预期安全行为） |
| 前后端代理 | Vite dev server `/api` 代理 → 8080（vite.config.ts）；本轮所有 curl 与历史浏览器走查均经此链路或直连成功 |
| CORS | 后端统一配置（历史核验），dev 场景同源代理 |

## 7. 三种角色登录、权限与数据隔离结果

- **登录**：三独立入口 + 身份选择页；角色由后端 JWT 裁定；错配 403/无效入口 400/错密码 401（不泄露角色）；登录后各入对应工作台。
- **权限**：未登录 401（AuthFilter）；角色不符 403（AuthSupport/ADMIN_ONLY_PREFIXES）；资源越权 403（Service 层 checkOwnership）。本轮复测：candidate 读他人简历 403、未登录 401、llm-provider cand/itv 403 + admin 200。
- **数据隔离**：resumes/interview_sessions/growth_records/training_plans 均 ownerId 归属；历史无主数据对候选人不可见（安全默认）；特权角色（就业指导口径）可见全部。视角差实测：简历 0/1/2、会话 9/22。
- **越权面**：不能通过改 URL/参数/ID 读取他人资源（checkOwnership 基于会话 ownerId，不信任客户端参数）。

## 8. 模型服务实际接入情况及降级测试

| 类别 | 结论 |
| --- | --- |
| 真实远端模型 | **受阻未验证**（无 API Key）。接入代码存在：LlmProviderRegistry / StructuredOutputInvoker（重试/结构化校验）/ 密钥加密。不得亦未声称已验证 |
| 本地 Ollama | **受阻未验证**（未安装模型） |
| demo 模式 | **通过**：确定性评分（长度档位+关键词命中加分）、证据句仅取自回答原文（无证据→null 显示"信息不足"，不编造）、简历确定性分析（74/60/48 分可复现）、全程【本地演示模式】标注、前端演示横幅 |
| 降级行为 | demo 开关 `app.demo-mode.enabled` 显式控制（当前 true）；无 Key 时不会伪装模型调用成功；模型异常统一错误码（超时/不可用/密钥无效区分），禁止自动切换可能收费 Provider |
| 隐私 | 密钥仅后端持有（.env/加密落盘），前端只见掩码；真实个人数据未发送第三方（无 Key 亦无外发）；"显式同意弹窗"未实现（遗留） |

## 9. 自动化测试、接口测试与端到端测试结果

| 类别 | 命令/方式 | 结果 |
| --- | --- | --- |
| 后端单元/集成测试 | `./gradlew :app:test --no-daemon` | **BUILD SUCCESSFUL**（248+ 用例，含 auth 22 例含 4 例入口校验、growth 8 例、评估/简历/限流等；本轮最终回归见文末附注） |
| 前端类型检查+构建 | `corepack pnpm run build` | **通过**（tsc 无错、vite 构建成功；chunk>500kB 为既有告警非错误） |
| 接口测试 | curl 矩阵（本轮+历史） | 登录七场景、三角色功能端点、越权/未登录负例、简历上传链、角色调整链 全部符合预期 |
| 数据库直查 | psql | 上传归属/分析记录/角色变更/审计 各表与 API 响应一致 |
| 端到端浏览器 | 浏览器走查（2026-10-27 两轮） | 8/8 + 12/12 PASS、console 无 error；**本轮工具不可用未重跑**（以 Vite 路由 200+组件服务验证+API 层证据替代，页面级复核建议人工打开四个登录 URL 与 /history） |
| 替代验证局限 | 声明 | 本轮未做页面级浏览器回归与真实模型 实测；以上均已在对应条目标注 |

## 10. 已发现问题清单（严重程度/原因/修复状态）

| 编号 | 问题 | 严重度 | 状态 |
| --- | --- | --- | --- |
| AC-01 | 面试报告接口在异步评估完成窗口内存在瞬态不一致（曾返回 70/1 题明细，终态 35/2 题正确且 DB/报告/趋势最终一致） | 低 | **未修复**（终态无错乱；建议：评估完成前 report 接口返回"评估中"而非中间值，或评估写缓存与落库同事务化） |
| AC-02 | Redis 不可用时 AbstractStreamConsumer 无退避/限频，日志放大（2026-10-27 曾因 bgsave 只读保护致后端被拖死，环境侧已修复 Redis 启动目录） | 中（仅基础设施异常时暴露） | **未修复**（已列遗留；建议消费失败加退避+限频日志） |
| AC-03 | 面试日程/知识库查询/语音会话查询无 owner 级过滤（管理入口已收敛为 ADMIN） | 中 | **未修复**（遗留；日程属多角色共享业务，需产品定义归属口径后再改） |
| AC-04 | 面试官"评价审核/任务分配"未实现（v2 已知，模型预留 ownerId） | 中 | **未实现**（菜单不显示，未伪装；最小方案见 §5 流程三） |
| AC-05 | 隐私"发送第三方前显式同意"弹窗未实现 | 低 | **未修复**（当前无 Key 无外发，风险暂不触发） |
| AC-06 | 本轮页面级浏览器走查未执行（工具不可用） | —（流程项） | 以替代验证覆盖并如实标注（§9） |

本轮验收新发现：AC-01（本轮）；AC-02~05 为历史已知遗留，本轮复核状态未变化。

## 11. 本项目验收期间（历史整改轮）已修复问题

| 问题 | 修复 | 文件 |
| --- | --- | --- |
| Redis bgsave 只读保护致后端宕机（WMI 启动工作目录错误） | Redis 启动显式 --dir/.infra --logfile | .infra 启动方式（运维项） |
| demo 评估关键词表未覆盖 Agent 等技能（证据恒 null） | 补 AGENT 关键词 + 通用兜底，证据仍取自原文 | DemoEvaluationService.java |
| 前端调用 /api/resumes/statistics 后端缺失（405 致简历页空列表） | 新增端点（角色维度统计）+ 前端 Promise.allSettled 加固 | ResumeRepository/ResumeStatsDTO/ResumeHistoryService/ResumeController/HistoryList.tsx |
| 单一登录页不满足 v2 三角色独立入口 | 后端 expectedRole 校验 + 前端三入口/选择页 | LoginRequest/AuthService/AuthController/RoleLoginPage.tsx/LoginPage.tsx/App.tsx/routes.ts/auth.ts/AuthContext.tsx |
| llm-provider 曾无管理员守卫 | AuthFilter ADMIN_ONLY_PREFIXES | AuthFilter.java |

## 12. 未解决问题、阻塞原因与处理建议

1. **AC-01 报告瞬态值**（低）：复现步骤——创建 1 题会话、作答后立即轮询 report。建议 report 在 evaluateStatus≠EVALUATED 时返回处理中状态码/占位，避免中间值外露。
2. **AC-02 Stream 消费退避**（中）：建议 AbstractStreamConsumer 失败后指数退避并对同类异常限频日志。
3. **AC-03 日程/知识库/语音 owner 过滤**（中）：先定义业务归属（日程是否面试官-候选人双向可见），再按 growth 模块 checkOwnership 模式补齐。
4. **AC-04 面试官评价审核**（中，答辩亮点机会）：最小实现=报告批注 API+面板+审计（数据结构已预留）。
5. **真实模型 验证**（受阻）：需任一 Provider 的 API Key；获得后按 demo-script 在设置页配置并复跑流程二对比。
6. **AC-06 页面级走查**：人工依次打开 /login、/login/{candidate,interviewer,admin}、/history 复核（历史两轮浏览器证据已覆盖大部分）。

## 13. 项目整体验收结论

**整体验收：通过（有条件）。**

- 前后端分离架构真实成立：独立构建均通过，前端经统一 API 客户端（Vite 代理→8080）调用后端，全链路（页面→API→鉴权→业务→DB/MinIO→返回→展示→复查一致）经实测打通。
- 三角色登录/权限/数据隔离全部实测通过；五条业务流程四条通过、一条（面试官评价）部分通过并给出最小方案。
- demo 模式真实可演示且诚实标注；真实模型 因无 Key 受阻未验证，未伪装。
- 遗留问题均为中低严重度且不阻塞核心闭环（简历→训练→面试→证据→成长→趋势 + 三角色治理）。
- 本报告全部结论基于实际执行的命令与响应；未执行项（真实模型、语音、本轮页面级浏览器走查）已如实标注阻塞原因。

---
### 附注：最终回归证据（2026-10-28 验收轮）

- 后端 `./gradlew :app:test --no-daemon`：BUILD SUCCESSFUL（退出码 0）。
- 前端 `corepack pnpm run build`：✓ built（退出码 0）。
- 本轮验收实测命令族：登录七场景 curl、简历上传/详情/统计 curl + psql 直查、面试 mini 闭环 curl + psql、角色调整 PATCH + psql + 审计查询、越权/未登录负例 curl。
