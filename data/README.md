# 模拟种子数据说明

> 数据库：`campus_recommend`（MySQL）
> 配套文档：`01-数据库设计.md`、`02-知识图谱设计.md`、`03-接口契约.md`

---

## 一、文件与执行顺序

| 文件 | 内容 | 顺序 |
|---|---|---|
| `01_schema.sql` | 18 张表的建表语句 | 1 |
| `02_college_major_course.sql` | 学院、专业、课程、资源分类 | 2 |
| `03_knowledge_point.sql` | 知识点编号说明，MySQL 没有知识点表，仅作参考 | 3 |
| `04_resource.sql` | 200 条资源、审核记录、Neo4j 同步日志 | 4 |
| `05_user.sql` | 2 个管理员、30 个学生 | 5 |
| `06_behavior.sql` | 收藏、下载、评分行为，ItemCF 的数据源 | 6 |
| `07_social.sql` | 评论、资源需求、问答、系统公告 | 7 |
| `neo4j_init.cypher` | Neo4j 的约束、索引、节点、关系 | 8 |

MySQL 按编号依次执行：

```bash
mysql -u root -p campus_recommend < 01_schema.sql
mysql -u root -p campus_recommend < 02_college_major_course.sql
# 03 只是说明文件，可跳过
mysql -u root -p campus_recommend < 04_resource.sql
# 05、06、07 同理
```

Neo4j 在 MySQL 之后执行：

```bash
cat neo4j_init.cypher | cypher-shell -u neo4j -p neo4j123
```

---

## 二、编号规则

两边对齐靠的就是这几个编号，改数据时不要动。

| 实体 | 编号 | 说明 |
|---|---|---|
| 学院 College | id 1 到 5，code 为 CS / INFO / SE / MATH / FL | MySQL `college.id` 等于 Neo4j `College.id` |
| 专业 Major | id 1 到 10 | 两侧一致 |
| 课程 Course | id 101 到 141，共 41 门 | 两侧一致 |
| 知识点 KnowledgePoint | id 1001 到 1060，共 60 个 | MySQL 用 `learning_resource.primary_kp_id` 引用，等于 Neo4j 的 `KnowledgePoint.id` |
| 学习资源 Resource | id 1 到 200，`resource_no` 为 `RES2026` 加四位序号 | `resource_no` 等于 Neo4j 的 `Resource.resource_id` |
| 学生 Student | id 1 到 30，学号 2023001 到 2023030 | 没有 |
| 管理员 Admin | id 1 到 2 | 没有 |

资源编号从 `RES20260001` 到 `RES20260200`。拿到 `resource_no`，就能在 MySQL 和 Neo4j 两边定位同一条资源。

---

## 三、数据规模

### MySQL

| 表 | 行数 | 表 | 行数 |
|---|---|---|---|
| college | 5 | learning_resource | 200，其中通过 192、待审 4、驳回 4 |
| major | 10 | resource_comment | 184 |
| course | 41 | favorite_behavior | 359 |
| resource_category | 16 | download_behavior | 264 |
| student_user | 30 | rating_behavior | 268 |
| admin | 2 | resource_requirement | 15 |
|  |  | requirement_reply | 22 |
|  |  | qa_post | 20 |
|  |  | qa_reply | 33 |
|  |  | system_announcement | 5 |
|  |  | resource_review_record | 396 |
|  |  | neo4j_sync_log | 200 |

### Neo4j

| 节点 | 数量 | 关系 | 数量 |
|---|---|---|---|
| College | 5 | BELONGS_TO，Major→College、Course→Major | 51 |
| Major | 10 | PREREQUISITE，Course→Course | 22 |
| Course | 41 | TEACHES，Course→KnowledgePoint | 60 |
| KnowledgePoint | 60 | PREREQUISITE / RELATED_TO / CONTAINS，KP→KP | 25 |
| Resource | 200 | BELONGS_TO_KP，Resource→KnowledgePoint | 200 |
| 合计 | 316 | 合计 | 358 |

---

## 四、演示时值得指出的几处

先确认 ItemCF 确实能跑。
20 个活跃学生产生了不少收藏、下载、评分行为。其中有 8 个"爆款"资源，比如数据结构、操作系统、算法、数据库、机器学习、高等数学、大学英语、Java 的代表资源，被不同学生反复收藏和下载，共现关系明显。余弦相似度矩阵不是空的，`/api/recommend/personal` 的 cf 路能返回结果。

想快速确认，可以跑：

```sql
SELECT resource_id, COUNT(*) c
FROM favorite_behavior
GROUP BY resource_id
ORDER BY c DESC
LIMIT 10;
```

图谱那条路也是通的。
从任意 `status=1` 的资源出发，沿 `BELONGS_TO_KP` 找到知识点，再经 `TEACHES` 上溯到课程，横向走 `RELATED_TO`，先修走 `PREREQUISITE`，多跳汇聚关联资源。可以直接用 `02` 文档第五节的 Cypher 试 `/api/recommend/related`。

热度是刻意拉开的。
`learning_resource.heat` 由浏览量、下载量、评分加权归一得到，范围在 0 到 1 之间，差距做得比较明显。`/api/recommend/hot` 和数据大屏的 `kp-heat`、`college-rank` 都能看出排序。

账号如下。
管理员 `admin / admin123` 是超级管理员，`manager / admin123` 是内容审核员。30 个学生账号密码都是 `123456`，用户名见 `05_user.sql`，比如 `linhaoran / 123456`，零行为用户可以用 `shaoziqian / 123456`。

审核和同步都有记录。
每条资源都有审核记录，提交、通过或驳回。通过的资源带一条 Neo4j 同步成功日志，驳回的带失败日志，可以用来演示 `02` 文档第六节的同步和重试机制。
