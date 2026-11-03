# 九项工程完善 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 按用户批准的九项验收标准完善校园推荐平台，保持简历 PDF 和既有技术路线不变。

**Architecture:** 保持 Spring Boot/MyBatis-Plus/MySQL/Neo4j 与 Vue 单体分层。采用最小必要业务修复、独立推荐计算单元、可重试同步记录及可复现的离线评测与基准测试；演示使用隔离数据，保留现有一键启动及端口隔离。

**Tech Stack:** Java 17+、Spring Boot 3.2.5、JUnit 5/Mockito、Vue 3/Vite、Python 3、GitHub Actions。

**Spec:** 本文下列 Global Constraints 及九项 Task 是用户在当前聊天中明确批准的验收清单的具体化。

## Global Constraints

- 不修改简历 PDF、其他项目文件和现有业务数据库中的用户数据。
- 保持三路推荐、五级知识图谱、学生端/管理端及既有接口路径。
- 密钥、邮件凭证、日志、缓存和运行状态不得提交；禁止伪造实验或性能结果。
- 根目录只保留一个一键启动入口；复用进程须验证归属；不得停止其他项目或共享数据库。
- 密码重置凭证必须安全投递、不可在 HTTP 响应或日志中泄漏，须一次有效且到期失效；未配置邮件时明确失败，不提供固定验证码后门。
- 测试使用 mock 或隔离数据库；评测按训练集生成行为统计，不允许测试集泄漏；明确模拟数据局限。
- 发布和 GitHub 资料更新在本次用户授权范围内，使用可用身份；无访问权限时记录具体阻碍，不虚报成功。
- 当前目录保留运行路径，通过 codex/ 前缀新分支隔离改动，不另建工作树。

## Task 1: 账号安全与状态

Files: `AuthService.java`, `AuthController.java`, `JwtInterceptor.java`, 新建重置凭证及邮件投递类、DTO、测试；`StudentForgot.vue`、`application.yml`、SQL migration。

- [ ] 编写并先运行缺失凭证、过期、重复使用、账号禁用/删除等失败测试。
- [ ] 实现随机凭证、哈希存储、到期校验、原子消费以及邮件安全投递；保留前端 code 字段。
- [ ] 登录和持有旧 JWT 的请求均校验账号有效状态；验证密码长度与角色边界。
- [ ] 执行 `mvn -f backend/pom.xml test`，记录测试输出并审查任务差异。

## Task 2: 推荐正确性

Files: `RecommendService.java`, 新建推荐计算单元及 `RecommendServiceTest.java` / 算法固定样例测试。

- [ ] 先用固定样例证明贡献来源、解释与舍入排序问题，运行失败测试。
- [ ] 来源按加权贡献判断，描述余弦相似度而非伪造人群比例，排序保持完整精度并使用稳定次序。
- [ ] 固定样例覆盖归一化、冷启动、过滤已交互资源及图谱不可用降级。
- [ ] 运行相关 JUnit 测试并保留原始输出，审查差异。

## Task 3: 启动交付

Files: `.startup-safe/launch.py`, `.startup-safe/frontend.mjs`, `.startup-safe/.gitignore`, `一键启动.bat`, `停止服务.bat`, `docs/quickstart.md`, `scripts/test_launcher.py`。

- [ ] 明确提交启动源码白名单，忽略全部状态、缓存、备份、凭据。
- [ ] 保留安全入口逻辑，修复可配置工具/数据库位置与可诊断的首次构建依赖。
- [ ] 旧停止入口统一到归属校验，不按通用 Vite 标记停止项目。
- [ ] 在临时全新克隆中执行构建和诊断，运行端口选择与归属判断测试。

## Task 4: 最低自动化

Files: `.github/workflows/ci.yml`, 后端测试目录、前端 `package.json` 和 `scripts/`。

- [ ] CI 使用 Java 17、Maven、Node 22、Python 3，执行后端测试、`npm ci`/`npm run build`、启动器测试。
- [ ] 覆盖鉴权、审核写入、推荐算法和图谱失败；避免使用本机真实服务作为 CI 前提。
- [ ] 本地执行等价命令；推送后核查 Actions 结果。

## Task 5: 推荐效果对照

Files: `evaluation/` 内脚本、固定数据和机器可读结果；`docs/evaluation.md`。

- [ ] 固定种子与划分，对 Hot、ItemCF、KG、Hybrid 使用相同候选、相同 K 和相同评测用户。
- [ ] 所有行为统计/相似度仅由训练集构造；冷启动用户的测试标签不能用于选兴趣种子。
- [ ] 评测使用产品推荐计算单元，输出 Recall@K、NDCG@K、catalog coverage 及冷启动分组。
- [ ] 运行至少一次完整评测，报告原始结果和限制，包括混合模型未胜出的结果。

## Task 6: 事务与图谱同步

Files: `ResourceAuditService.java`, `ResourceService.java`, `Neo4jSyncService.java`, `Neo4jSyncLog.java`, 对应管理端接口/页面和测试。

- [ ] 先测试审核中间失败回滚、同步失败可见、重试和重复执行。
- [ ] 同一 MySQL 多步写入添加事务；跨库同步放在提交后执行，保留待处理/成功/失败日志。
- [ ] 管理员可查询和重试失败同步；重复 MERGE 保持幂等，审核事务回滚时不得入图。
- [ ] 执行故障注入测试和隔离数据库集成测试。

## Task 7: 性能测量与优化

Files: 推荐计算/快照缓存类、`benchmark/`、`docs/performance.md`。

- [ ] 在同一代码、同一生成数据、硬件和并发下运行无缓存基线。
- [ ] 使用有界缓存或批量加载复用相似度计算；明确 TTL/失效策略与数据新鲜度。
- [ ] 测试并发一致性、缓存命中和到期刷新；运行优化后基准并报告 P50/P95、吞吐、误差与测量层级。
- [ ] 不把内存算法基准写成 HTTP 或线上生产性能。

## Task 8: 招聘展示

Files: `README.md`, `docs/demo/`、`docs/contribution.md`, `docs/release-notes.md`。

- [ ] README 前屏说明定位、作者负责范围、三个技术难点和可复现验证入口。
- [ ] 数字由实际统计生成；保留简历原文件，记录历史数字口径差异。
- [ ] 录制约两分钟真实浏览器流程视频：资源、推荐/冷启动、图谱、管理审核和同步；在隔离演示数据中完成写操作。
- [ ] 检查视频时长、关键画面和资源链接可用，不把动画幻灯片冒充系统实录。

## Task 9: GitHub 交付

Files: 发布说明及账号公开字段（通过 GitHub API）。

- [ ] 明确变更差异，运行全套验证，提交源码及机器可读评测结果。
- [ ] 拉取最新远端，以不覆盖用户更新的方式推送，固定发布 tag 和 release。
- [ ] 更新个人简介和固定主项目（取决于已有 GitHub 身份权限），核对发布页面和 CI。
- [ ] 完整交付可执行命令、测试计数、评测/性能实测结果和未完成外部动作。

## Progress ledger

- Started: 基线为本地 `2f11d96`，存在用户前序授权的启动器变更，必须保留。
- Decision: 简历保持原样；新结果只写 README、评测报告和发布说明。
