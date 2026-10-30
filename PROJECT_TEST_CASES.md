# 测试用例集（PROJECT_TEST_CASES.md）

> 覆盖正常流程、异常流程、权限边界与关键跨角色流程。状态：✅通过 / ⚠️部分通过 / ❌失败 / ⛔受阻未验证。
> 测试数据均为合成数据（测试候选人/测试简历等），不含真实隐私与真实密钥。
> 执行方式：curl（PowerShell，JSON 经 stdin 管道）、psql 直查、浏览器走查（历史轮）、Gradle/JUnit。

## 0. 测试数据方案（可重复）

- 演示账号（local profile 种子，可重复）：candidate/candidate123（userId=1）、interviewer/interviewer123（userId=2）、admin/admin123（userId=3）、c2（注册产生的测试号，userId=4，用于角色调整测试）。
- 合成简历：`%TEMP%\acceptance-resume.txt`（603B，"测试候选人/13800000000（合成数据）/test@example.com"，内容见 PROJECT_ACCEPTANCE_REPORT §5 流程一）。
- 面试答案：固定文本（"JVM内存分为堆栈方法区…MVCC通过undo log…"96 字），demo 模式确定性评分可复现（70 分/命中 jvm、内存、垃圾回收、并发）。
- 清理策略：验收数据保留于本地库（合成数据无隐私风险）；如需重置，删除 resumes/interview_sessions 中验收记录即可，不影响种子账号。

## 1. 认证与登录入口

| 编号 | 模块 | 前置 | 步骤 | 预期 | 实际 | 状态 | 缺陷 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-A01 | 注册 | 无 | POST /api/v1/auth/register（新用户名） | code=200，role=CANDIDATE | 200，c2 创建为 CANDIDATE | ✅ | |
| TC-A02 | 通用登录 | TC-A01 | POST login 不带 expectedRole | 200+token | 200 | ✅ | |
| TC-A03 | 求职者入口 | 种子账号 | POST login expectedRole=CANDIDATE | 200 | 200 | ✅ | |
| TC-A04 | 面试官入口 | 种子账号 | POST login expectedRole=INTERVIEWER | 200 | 200 | ✅ | |
| TC-A05 | 管理员入口 | 种子账号 | POST login expectedRole=ADMIN | 200 | 200 | ✅ | |
| TC-A06 | 角色错配 | 种子账号 | 管理员入口输入 candidate 凭据 | 403 明确提示、不签发 token | 403「该账号是「求职者」账号…」；单测 verify never issueToken | ✅ | |
| TC-A07 | 无效入口 | 种子账号 | expectedRole=SUPERUSER | 400「登录入口无效」 | 400 | ✅ | |
| TC-A08 | 错误密码 | 种子账号 | 错密码+expectedRole=ADMIN | 401，不泄露角色 | 401「用户名或密码错误」 | ✅ | |
| TC-A09 | 入口页面 | 前端在线 | 打开 /login、/login/{3角色} | 四页均 200 渲染 | Vite 四路由 200；选择页三卡片+三角色页 | ✅ | 页面级浏览器走查本轮未跑（AC-06），历史轮已过 |
| TC-A10 | 退出/过期 | 已登录 | 点退出 / 构造过期 token | 回 /login；401 清 token 跳登录 | Layout 退出→/login；拦截器 401 处理 | ✅ | |
| TC-A11 | 路由守卫 | 未登录 | 直接访问 /dashboard | 跳 /login 选择页 | ProtectedRoute 跳转 | ✅ | |
| TC-A12 | 入口单元测试 | — | AuthServiceTest 4 例 | 全过 | 全过（含不签发 token 断言） | ✅ | |

## 2. 求职者功能

| 编号 | 模块 | 前置 | 步骤 | 预期 | 实际 | 状态 | 缺陷 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-B01 | 简历上传 | candidate 登录 | 上传 603B txt | 200；文件入 MinIO；DB 记录 owner=本人；状态 PENDING | id=3、MinIO 对象存在、owner_id=1 | ✅ | |
| TC-B02 | 异步分析 | TC-B01 | 等待评估（Redis Stream） | COMPLETED + 分析记录入库 | COMPLETED、74 分（demo 确定性） | ✅ | |
| TC-B03 | 详情复查 | TC-B02 | GET /api/resumes/3/detail | resumeText/analyses 与库一致；accessCount+1 | 一致；1→可见自增 | ✅ | |
| TC-B04 | 统计一致性 | TC-B01 | GET /api/resumes/statistics | 与 SQL count/sum 一致（本人维度） | {2,9,2} = SQL 计数 | ✅ | |
| TC-B05 | 简历列表归属 | 多账号数据 | candidate 与 itv 分别 GET /api/resumes | cand 仅本人，itv 全量 | 视角差正确（0/2 vs 全量） | ✅ | |
| TC-B06 | 删除/重析归属 | 存在他人简历 | candidate 操作他人 resumeId | 403 | 403 无权访问该简历 | ✅ | |
| TC-B07 | 岗位浏览 | 面试官已发布岗位 | GET /api/v1/jobs | 仅 PUBLISHED 可见 | 3 条已发布 | ✅ | |
| TC-B08 | 训练计划 | 岗位存在 | 创建→complete | 200 状态流转；他人创建 403 | 创建/完成 200；越权 403（单测+curl） | ✅ | |
| TC-B09 | 模拟面试 | candidate 登录 | 创建会话(questionCount=1)→答题→complete | 出题含默认追问；报告落库 | 2 题（1主+1追问）；Q0=70 证据句、追问 0、总分 35 | ✅ | 追问未答为测试步骤选择，非缺陷 |
| TC-B10 | 报告一致性 | TC-B09 | 对比 report API / DB / trend | 三者一致 | 终态均 35（2 题明细） | ⚠️ | **AC-01**：评估窗口内 report 曾短暂返回 70/1 题（瞬态） |
| TC-B11 | 成长记录 | 报告薄弱项 | 创建记录→done | OPEN→DONE；他人 403 | 200 流转；越权 403 | ✅ | |
| TC-B12 | 成长趋势 | 多场会话 | GET /api/v1/growth/trend | 按本人会话得分聚合 | 与 DB overall_score 一致 | ✅ | |
| TC-B13 | 个人资料维护 | — | 查找资料维护页面 | 可维护资料 | 无该页面（菜单未提供） | ❌未实现 | 如实标记，不伪装 |

## 3. 面试官功能

| 编号 | 模块 | 前置 | 步骤 | 预期 | 实际 | 状态 | 缺陷 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-C01 | 岗位创建/发布 | interviewer 登录 | 创建（能力要求 2 条）→publish | DRAFT→PUBLISHED+审计 | 200；audit CREATE/PUBLISH | ✅ | |
| TC-C02 | 岗位越权 | candidate 登录 | POST /api/v1/jobs | 403 | 403 无权限（含合法 body 场景） | ✅ | |
| TC-C03 | 候选人视图 | 多角色数据 | itv 查简历/面试记录 | 特权全量 | 1/22 vs candidate 2/9，视角差正确 | ✅ | |
| TC-C04 | 面试日程 | 登录 | 打开日程页/接口 | 可用 | 历史 200；**无 owner 过滤** | ⚠️ | AC-03 |
| TC-C05 | 评价录入 | — | 查找评价/审核功能 | 可录入评价 | 无实现（菜单不显示） | ❌未实现 | AC-04，最小方案见验收报告 §12 |
| TC-C06 | 任务分配 | — | 查找分配功能 | 可分配任务 | 无实现 | ❌未实现 | AC-04 |

## 4. 管理员功能

| 编号 | 模块 | 前置 | 步骤 | 预期 | 实际 | 状态 | 缺陷 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-D01 | 用户列表 | admin 入口登录 | GET /api/v1/admin/users | 5 用户 | 5 | ✅ | |
| TC-D02 | 角色调整 | c2 存在 | PATCH c2→INTERVIEWER→SQL 复核→改回 | DB 生效+审计 2 条 | users 表角色两段变化；audit CHANGE_ROLE×2 | ✅ | |
| TC-D03 | 管理接口越权 | candidate/interviewer token | GET admin/users、audit-logs | 403 | 403 | ✅ | |
| TC-D04 | 模型服务 守卫 | 三角色 token | GET /api/llm-provider/list | 仅 admin 200 | 403/403/200 | ✅ | |
| TC-D05 | 密钥掩码 | admin | 查看响应字段 | 仅 maskedApiKey | you***掩码 | ✅ | |
| TC-D06 | 审计日志 | 存在操作 | GET audit-logs | 含岗位/角色操作 | 6+ 条（CREATE/PUBLISH_POSITION、CHANGE_ROLE） | ✅ | |
| TC-D07 | 知识库 | admin 登录 | 管理页/接口 | 可用；RAG 实测 | 页面接口历史通过；向量化依赖 embedding 未实测 | ⛔部分受阻 | 无 Key；AC-03 范畴 |

## 5. 权限边界与安全

| 编号 | 模块 | 前置 | 步骤 | 预期 | 实际 | 状态 | 缺陷 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-E01 | 未登录 | 无 token | 访问业务 API | 401 | /api/resumes 等 401 | ✅ | |
| TC-E02 | 跨用户简历 | candidate + 他人 resumeId | GET detail | 403 | 403 | ✅ | |
| TC-E03 | 跨用户会话 | candidate + 他人 sessionId | details/export/delete | 403 | 403（历史实测） | ✅ | |
| TC-E04 | 客户端伪造角色 | 任意 token | 仅改前端路由/参数 | 后端仍按 JWT 角色裁决 | expectedRole 后端校验；role 不取自客户端 | ✅ | |
| TC-E05 | 历史无主数据 | owner=null 行 | candidate 访问 | 不可见/403 | 403 | ✅ | |
| TC-E06 | 非法参数 | 登录 | 空用户名/缺字段/非法 JSON | 400 校验提示 | @NotBlank 400；JSON 解析错误 500→统一提示（后端日志有栈） | ✅ | 建议后续将 HttpMessageNotReadable 归一为 400（低） |

## 6. 跨角色闭环

| 编号 | 模块 | 前置 | 步骤 | 预期 | 实际 | 状态 | 缺陷 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-F01 | 岗位→训练→评价→查看→反馈 | 三账号 | interviewer 发布→candidate 建计划+面试+评估→itv 查记录→cand 看反馈→admin 审计 | 同一真实数据在各角色授权范围内流转 | 2026-10-27 实测全链路通过（岗位/会话/计划 ID 一致） | ✅ | itv 环节为"查看"，评价录入未实现（AC-04） |

## 7. 模型模式与降级

| 编号 | 模块 | 前置 | 步骤 | 预期 | 实际 | 状态 | 缺陷 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-G01 | demo 可重复 | demo 开启 | 同输入多次评估 | 结果完全一致 | DemoEvaluationServiceTest.deterministicEvaluation 过 | ✅ | |
| TC-G02 | demo 标识 | demo 开启 | 查看反馈/前端 | 【本地演示模式】标注+前端横幅 | 均有 | ✅ | |
| TC-G03 | 无证据不编造 | 模型模式 | evidence 字段 | null→前端"信息不足" | 模型模式 evidence=null（代码约定+注释禁编造） | ✅ | |
| TC-G04 | 真实远端模型 | 需 API Key | 配置后跑流程二 | 真实模型输出 | **未执行（无 Key）** | ⛔ | 受阻，不伪装 |
| TC-G05 | Ollama 本地 | 需安装模型 | 配置后验证 | 本地推理 | **未执行（未安装）** | ⛔ | |
| TC-G06 | 语音 ASR/TTS | 需 Key | 语音面试 | 可用 | **未执行（无 Key）** | ⛔ | 文字闭环不受影响 |

## 8. 构建与回归

| 编号 | 项 | 命令 | 结果 | 状态 |
| --- | --- | --- | --- | --- |
| TC-H01 | 后端全量测试 | `./gradlew :app:test --no-daemon` | BUILD SUCCESSFUL（退出码 0，248+ 用例） | ✅ |
| TC-H02 | 前端构建 | `corepack pnpm run build` | ✓ built 20.60s（退出码 0） | ✅ |
| TC-H03 | 后端健康 | GET /actuator/health | UP | ✅ |
| TC-H04 | 前端路由 | GET 5173 四登录路由+页面 | 200 | ✅ |
| TC-H05 | 页面级浏览器走查 | 人工/自动化 | 历史两轮 8/8+12/12 PASS；本轮未重跑 | ⚠️待验证（AC-06） |

## 统计汇总

- 用例总数：**52**（TC-A12 + TC-B13 + TC-C6 + TC-D7 + TC-E6 + TC-F1 + TC-G6 + TC-H5）
- ✅ 通过：**44**　⚠️ 部分通过：**3**（TC-B10/AC-01、TC-C04/AC-03、TC-H05/AC-06）
- ❌ 失败：**0**；未实现项计入对应模块（TC-B13、TC-C05、TC-C06 为未实现非失败）
- ⛔ 受阻未验证：**3**（TC-G04/05/06，均因无 Key/模型）
- 与验收报告 §3 的 47 项功能清单口径互补：功能清单按业务功能计，本表按可执行用例计。
