# 模拟种子数据说明（毕业设计第六套）

> 系统：基于知识图谱 + 协同过滤算法的校园学习资源推荐平台
> 配套文档：`01-数据库设计.md`（MySQL）、`02-知识图谱设计.md`（Neo4j）、`03-接口契约.md`（API）

## 一、文件清单与执行顺序

| 文件 | 内容 | 执行顺序 |
|------|------|---------|
| `01_schema.sql` | 全部 18 张 MySQL 表建表语句（严格照抄 01 文档） | 1 |
| `02_college_major_course.sql` | 学院 / 专业 / 课程 / 资源分类 | 2 |
| `03_knowledge_point.sql` | 知识点编号说明（MySQL 无 KP 表，仅文档参考） | 3 |
| `04_resource.sql` | 学习资源 200 条 + 资源审核记录 + Neo4j 同步日志 | 4 |
| `05_user.sql` | 管理员 2 + 学生 30 | 5 |
| `06_behavior.sql` | 收藏 / 下载 / 评分行为（ItemCF 数据源） | 6 |
| `07_social.sql` | 评论 / 资源需求 / 问答 / 系统公告 | 7 |
| `neo4j_init.cypher` | Neo4j 约束/索引/节点/关系（在 MySQL 之后执行） | 8 |

**推荐执行流程**
1. MySQL：依次执行 `01_schema.sql` → `02` → `03` → `04` → `05` → `06` → `07`（每个文件可独立 source）。
2. Neo4j：执行 `neo4j_init.cypher`（建议用 Neo4j Browser 或 `cypher-shell`）。

```sql
-- MySQL 示例
mysql -u root -p campus_db < 01_schema.sql
mysql -u root -p campus_db < 02_college_major_course.sql
...
```

```bash
# Neo4j 示例（cypher-shell）
cat neo4j_init.cypher | cypher-shell -u neo4j -p password
```

## 二、编号规则（两侧一致性的关键）

| 实体 | 编号方案 | 说明 |
|------|---------|------|
| 学院 College | `id` 1..5；`code` CS / INFO / SE / MATH / FL | MySQL `college.id` == Neo4j `College.id` |
| 专业 Major | `id` 1..10 | MySQL `major.id` == Neo4j `Major.id` |
| 课程 Course | `id` 101..141（共 41 门） | MySQL `course.id` == Neo4j `Course.id` |
| 知识点 KnowledgePoint | `id` 1001..1060（共 60 个） | MySQL `learning_resource.primary_kp_id` 引用；== Neo4j `KnowledgePoint.id` |
| 学习资源 Resource | `id` 1..200；`resource_no` = `RES2026` + 4 位序号 | **`resource_no`(MySQL) == `Resource.resource_id`(Neo4j)**，全局唯一一致 |
| 学生 Student | `id` 1..30；`student_no` 2023001..2023030 | |
| 管理员 Admin | `id` 1..2 | |

> 资源编号示例：`RES20260001` … `RES20260200`。只要拿到 `resource_no`，即可在 MySQL 与 Neo4j 两侧定位同一条资源。

## 三、数据规模统计

### MySQL 各表行数
| 表 | 行数 | 表 | 行数 |
|----|-----|----|-----|
| college | 5 | learning_resource | 200（通过 192 / 待审 4 / 驳回 4） |
| major | 10 | resource_comment | 184 |
| course | 41 | favorite_behavior | 359 |
| resource_category | 16 | download_behavior | 264 |
| student_user | 30 | rating_behavior | 268 |
| admin | 2 | resource_requirement | 15 |
| | | requirement_reply | 22 |
| | | qa_post | 20 |
| | | qa_reply | 33 |
| | | system_announcement | 5 |
| | | resource_review_record | 396 |
| | | neo4j_sync_log | 200 |

### Neo4j 图谱规模
| 节点标签 | 数量 | 关系类型 | 数量 |
|---------|-----|---------|-----|
| College | 5 | BELONGS_TO（Major→College, Course→Major） | 51 |
| Major | 10 | PREREQUISITE（Course→Course） | 22 |
| Course | 41 | TEACHES（Course→KnowledgePoint） | 60 |
| KnowledgePoint | 60 | PREREQUISITE/RELATED_TO/CONTAINS（KP→KP） | 25 |
| Resource | 200 | BELONGS_TO_KP（Resource→KnowledgePoint） | 200 |
| **节点合计** | **316** | **边合计** | **358** |

## 四、演示要点提示

1. **ItemCF 协同过滤能跑通（关键）**
   - 20 个活跃学生产生了大量「收藏/下载/评分」共现行为；其中 8 个「爆款资源」（数据结构、操作系统、算法、数据库、机器学习、高等数学、大学英语、Java 等热门课的代表资源）被多个不同学生反复收藏/下载，可在 `favorite_behavior` / `download_behavior` 中明显看到共现，ItemCF 余弦相似度矩阵非空，`/api/recommend/personal` 的 `cf` 路能返回结果。
   - 验证 SQL：`SELECT resource_id, COUNT(*) c FROM favorite_behavior GROUP BY resource_id ORDER BY c DESC LIMIT 10;` 可见高共现资源。

2. **知识图谱语义推荐（kg 路）**
   - 从任意 `status=1` 的资源出发，沿 `BELONGS_TO_KP` 找到知识点，再经 `TEACHES` 上溯到课程、横向 `RELATED_TO`、先修 `PREREQUISITE`，多跳汇聚关联资源。可直接用 `02` 文档 §五的 Cypher 体验 `/api/recommend/related`。

3. **热度推荐（hot 路）**
   - `learning_resource.heat` 由浏览量/下载量/评分加权归一得到，刻意拉开差距（0.00~1.00），`/api/recommend/hot` 与数据大屏（`/api/admin/dashboard/kp-heat`、`college-rank`）能看出明显排序。

4. **账号**
   - 管理员：`admin / admin123`（超级管理员）、`manager / admin123`（内容审核员）。
   - 学生：30 个账号密码统一为 `123456`（已 BCrypt 加密），用户名见 `05_user.sql`，例如 `linhaoran / 123456`。

5. **审核流与同步**
   - 每个资源都带 `resource_review_record`（提交+通过/驳回），通过者带 `neo4j_sync_log` 成功记录；被驳回资源带失败日志，可演示 02 文档 §六 的同步/重试机制。
