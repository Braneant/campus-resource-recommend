# -*- coding: utf-8 -*-
"""读取 gen.py 生成的 in-memory 数据，写出所有 SQL / Cypher / README 文件。"""
import gen
import os

OUT = gen.OUT

colleges = gen.colleges
majors = gen.majors
courses = gen.courses
categories = gen.categories
knowledge_points = gen.knowledge_points
kp_rels = gen.kp_rels
resources = gen.resources
students = gen.students
admins = gen.admins
fav = gen.fav
dl = gen.dl
rate = gen.rate
comments = gen.comments
requirements = gen.requirements
req_replies = gen.req_replies
qa_posts = gen.qa_posts
qa_replies = gen.qa_replies
ann_list = gen.ann_list
review_records = gen.review_records
sync_logs = gen.sync_logs

S = gen.sq
C = gen.cy
rand_dt = gen.rand_dt

def W(name, lines):
    """写出文件，lines 为字符串或可迭代"""
    path = os.path.join(OUT, name)
    with open(path, "w", encoding="utf-8") as f:
        if isinstance(lines, str):
            f.write(lines)
        else:
            for ln in lines:
                f.write(ln)
    print("written", name)

# ============================================================
# 01_schema.sql  —— 建表（严格照抄 01-数据库设计.md）
# ============================================================
schema = r"""-- ============================================================
-- 01_schema.sql  建表语句（严格依据 01-数据库设计.md）
-- 字符集 utf8mb4 / 排序规则 utf8mb4_0900_ai_ci
-- 执行顺序：本文件 -> 02..07 -> neo4j_init.cypher
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE college (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  name         VARCHAR(100) NOT NULL,
  code         VARCHAR(50)  NOT NULL,
  description  VARCHAR(255),
  sort         INT          NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学院表';

CREATE TABLE major (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  college_id   BIGINT       NOT NULL,
  name         VARCHAR(100) NOT NULL,
  code         VARCHAR(50)  NOT NULL,
  description  VARCHAR(255),
  sort         INT          NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_college_id (college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专业表';

CREATE TABLE course (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  college_id   BIGINT        NOT NULL,
  major_id     BIGINT,
  name         VARCHAR(100)  NOT NULL,
  code         VARCHAR(50)   NOT NULL,
  teacher      VARCHAR(50),
  credit       DECIMAL(3,1)  NOT NULL DEFAULT 0.0,
  semester     VARCHAR(20),
  description  TEXT,
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_college_id (college_id),
  KEY idx_major_id (major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程表';

CREATE TABLE resource_category (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  name         VARCHAR(50)  NOT NULL,
  parent_id    BIGINT,
  sort         INT          NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源分类表（支持二级）';

CREATE TABLE learning_resource (
  id             BIGINT         NOT NULL AUTO_INCREMENT,
  resource_no    VARCHAR(50)    NOT NULL,
  title          VARCHAR(200)   NOT NULL,
  category_id    BIGINT,
  college_id     BIGINT,
  major_id       BIGINT,
  course_id      BIGINT,
  primary_kp_id  BIGINT,
  type           TINYINT        NOT NULL DEFAULT 1,
  file_url       VARCHAR(255),
  cover_url      VARCHAR(255),
  description    TEXT,
  author         VARCHAR(100),
  uploader_id    BIGINT,
  uploader_type  TINYINT        NOT NULL DEFAULT 1,
  view_count     INT            NOT NULL DEFAULT 0,
  download_count INT            NOT NULL DEFAULT 0,
  favorite_count INT            NOT NULL DEFAULT 0,
  avg_rating     DECIMAL(3,2)   NOT NULL DEFAULT 0.00,
  rating_count   INT            NOT NULL DEFAULT 0,
  heat           DECIMAL(10,4)  NOT NULL DEFAULT 0.0000,
  status         TINYINT        NOT NULL DEFAULT 0,
  audit_time     DATETIME,
  create_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_no (resource_no),
  KEY idx_category_id (category_id),
  KEY idx_college_id (college_id),
  KEY idx_course_id (course_id),
  KEY idx_primary_kp_id (primary_kp_id),
  KEY idx_status (status),
  KEY idx_heat (heat),
  KEY idx_view_count (view_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习资源表';

CREATE TABLE resource_comment (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  resource_id  BIGINT       NOT NULL,
  student_id   BIGINT       NOT NULL,
  content      VARCHAR(500) NOT NULL,
  parent_id    BIGINT,
  like_count   INT          NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_resource_id (resource_id),
  KEY idx_student_id (student_id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源评论表';

CREATE TABLE favorite_behavior (
  id           BIGINT     NOT NULL AUTO_INCREMENT,
  student_id   BIGINT     NOT NULL,
  resource_id  BIGINT     NOT NULL,
  create_time  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fav (student_id, resource_id),
  KEY idx_resource_id (resource_id),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏行为表(ItemCF)';

CREATE TABLE download_behavior (
  id           BIGINT     NOT NULL AUTO_INCREMENT,
  student_id   BIGINT     NOT NULL,
  resource_id  BIGINT     NOT NULL,
  create_time  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dl (student_id, resource_id),
  KEY idx_resource_id (resource_id),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='下载行为表(ItemCF)';

CREATE TABLE rating_behavior (
  id           BIGINT     NOT NULL AUTO_INCREMENT,
  student_id   BIGINT     NOT NULL,
  resource_id  BIGINT     NOT NULL,
  score        TINYINT    NOT NULL,
  create_time  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rate (student_id, resource_id),
  KEY idx_resource_id (resource_id),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评分行为表(ItemCF显式)';

CREATE TABLE student_user (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  student_no   VARCHAR(20)  NOT NULL,
  username     VARCHAR(50)  NOT NULL,
  password     VARCHAR(100) NOT NULL,
  nickname     VARCHAR(50),
  real_name    VARCHAR(50),
  gender       TINYINT      NOT NULL DEFAULT 0,
  college_id   BIGINT,
  major_id     BIGINT,
  grade        VARCHAR(20),
  email        VARCHAR(100),
  phone        VARCHAR(20),
  avatar       VARCHAR(255),
  status       TINYINT      NOT NULL DEFAULT 1,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_student_no (student_no),
  KEY idx_college_id (college_id),
  KEY idx_major_id (major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生用户表';

CREATE TABLE admin (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  username        VARCHAR(50)  NOT NULL,
  password        VARCHAR(100) NOT NULL,
  real_name       VARCHAR(50),
  role            TINYINT      NOT NULL DEFAULT 2,
  email           VARCHAR(100),
  phone           VARCHAR(20),
  status          TINYINT      NOT NULL DEFAULT 1,
  last_login_time DATETIME,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员表';

CREATE TABLE resource_requirement (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  student_id   BIGINT       NOT NULL,
  title        VARCHAR(200) NOT NULL,
  content      TEXT         NOT NULL,
  category_id  BIGINT,
  college_id   BIGINT,
  status       TINYINT      NOT NULL DEFAULT 0,
  reply_count  INT          NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_student_id (student_id),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源需求表(互助)';

CREATE TABLE requirement_reply (
  id             BIGINT     NOT NULL AUTO_INCREMENT,
  requirement_id BIGINT     NOT NULL,
  student_id     BIGINT     NOT NULL,
  content        TEXT       NOT NULL,
  like_count     INT        NOT NULL DEFAULT 0,
  create_time    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_requirement_id (requirement_id),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='需求回复表';

CREATE TABLE qa_post (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  student_id   BIGINT       NOT NULL,
  title        VARCHAR(200) NOT NULL,
  content      TEXT         NOT NULL,
  category_id  BIGINT,
  tags         VARCHAR(255),
  view_count   INT          NOT NULL DEFAULT 0,
  reply_count  INT          NOT NULL DEFAULT 0,
  like_count   INT          NOT NULL DEFAULT 0,
  status       TINYINT      NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_student_id (student_id),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问答帖子表';

CREATE TABLE qa_reply (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  post_id      BIGINT       NOT NULL,
  student_id   BIGINT       NOT NULL,
  content      TEXT         NOT NULL,
  parent_id    BIGINT,
  like_count   INT          NOT NULL DEFAULT 0,
  is_best      TINYINT      NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_post_id (post_id),
  KEY idx_student_id (student_id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问答回复表';

CREATE TABLE system_announcement (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  admin_id        BIGINT,
  title           VARCHAR(200) NOT NULL,
  content         TEXT         NOT NULL,
  priority        TINYINT      NOT NULL DEFAULT 1,
  publish_status  TINYINT      NOT NULL DEFAULT 0,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  publish_time    DATETIME,
  PRIMARY KEY (id),
  KEY idx_publish_status (publish_status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统公告表';

CREATE TABLE resource_review_record (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  resource_id     BIGINT       NOT NULL,
  submitter_id    BIGINT,
  reviewer_id     BIGINT,
  action          TINYINT      NOT NULL,
  review_comment  VARCHAR(500),
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_resource_id (resource_id),
  KEY idx_reviewer_id (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源审核记录表';

CREATE TABLE neo4j_sync_log (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  resource_id  BIGINT       NOT NULL,
  sync_type    TINYINT      NOT NULL,
  sync_status  TINYINT      NOT NULL DEFAULT 0,
  error_msg    VARCHAR(500),
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_resource_id (resource_id),
  KEY idx_sync_status (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Neo4j同步日志表';

SET FOREIGN_KEY_CHECKS=1;
"""
W("01_schema.sql", schema)

# ============================================================
# 02_college_major_course.sql
# ============================================================
lines = ["-- 02_college_major_course.sql  学院 / 专业 / 课程 / 资源分类\n",
         "SET FOREIGN_KEY_CHECKS=0;\n"]
lines.append("\n-- 学院\n")
lines.append("INSERT INTO college (id, name, code, description, sort, create_time) VALUES\n")
lines.append(",\n".join(
    f"({cid}, {S(name)}, {S(code)}, {S(desc)}, {i}, {S(rand_dt('2025-01-01 08:00:00','2025-03-01 08:00:00'))})"
    for i, (cid, name, code, desc) in enumerate(colleges, start=1)
) + ";\n")
lines.append("\n-- 专业\n")
lines.append("INSERT INTO major (id, college_id, name, code, description, sort, create_time) VALUES\n")
lines.append(",\n".join(
    f"({mid}, {col}, {S(name)}, {S(code)}, {S(desc)}, {i}, {S(rand_dt('2025-01-01 08:00:00','2025-03-01 08:00:00'))})"
    for i, (mid, col, name, code, desc) in enumerate(majors, start=1)
) + ";\n")
maj_map = {m[0]: m[2] for m in majors}
lines.append("\n-- 学院\nINSERT INTO college (id, name, code, description, sort, create_time) VALUES\n")
lines.append(",\n".join(
    f"({cid}, {S(name)}, {S(code)}, {S(desc)}, {i}, {S(rand_dt('2025-01-01 08:00:00','2025-03-01 08:00:00'))})"
    for i, (cid, name, code, desc) in enumerate(colleges, start=1)
) + ";\n")
lines.append("\n-- 专业\nINSERT INTO major (id, college_id, name, code, description, sort, create_time) VALUES\n")
lines.append(",\n".join(
    f"({mid}, {col}, {S(name)}, {S(code)}, {S(desc)}, {i}, {S(rand_dt('2025-01-01 08:00:00','2025-03-01 08:00:00'))})"
    for i, (mid, col, name, code, desc) in enumerate(majors, start=1)
) + ";\n")
lines.append("\n-- 课程\nINSERT INTO course (id, college_id, major_id, name, code, teacher, credit, semester, description, create_time) VALUES\n")
lines.append(",\n".join(
    f"({cid}, {col}, {maj}, {S(name)}, {S(code)}, {S(teacher)}, {credit}, {S(sem)}, {S(name+'是'+maj_map.get(maj,'本专业')+'的核心课程，系统讲授相关理论与实验。')}, {S(rand_dt('2025-02-01 08:00:00','2025-04-01 08:00:00'))})"
    for (cid, col, maj, name, code, teacher, credit, sem) in courses
) + ";\n")
lines.append("\n-- 资源分类（二级）\nINSERT INTO resource_category (id, name, parent_id, sort, create_time) VALUES\n")
lines.append(",\n".join(
    f"({cid}, {S(name)}, {S(pid) if pid else 'NULL'}, {i}, {S(rand_dt('2025-01-01 08:00:00','2025-02-01 08:00:00'))})"
    for i, (cid, name, pid) in enumerate(categories, start=1)
) + ";\n")
lines.append("SET FOREIGN_KEY_CHECKS=1;\n")
W("02_college_major_course.sql", lines)

# ============================================================
# 03_knowledge_point.sql  (MySQL 无 KP 表，仅作编号说明与引用)
# ============================================================
lines = ["-- 03_knowledge_point.sql\n",
         "-- 说明：依据 01-数据库设计.md，MySQL 业务库中【不存在】独立的知识点表；\n",
         "-- 知识点（KnowledgePoint）为图谱侧实体，统一存于 Neo4j（见 neo4j_init.cypher）。\n",
         "-- MySQL 侧的 learning_resource.primary_kp_id 以 BIGINT 冗余引用 Neo4j KnowledgePoint.id。\n\n",
         "-- 下方为知识点与课程的归属映射（仅文档参考，不直接写入 MySQL）：\n"]
for k in knowledge_points:
    c = gen.course_by_id.get(k["course_id"])
    cname = c[3] if c else "?"
    lines.append(f"-- KP id={k['id']:>5}  {k['name']:<8}  course_id={k['course_id']} ({cname})\n")
W("03_knowledge_point.sql", lines)

# ============================================================
# 04_resource.sql  (资源 + 审核记录 + 同步日志)
# ============================================================
lines = ["-- 04_resource.sql  学习资源 + 资源审核记录 + Neo4j 同步日志\n",
         "SET FOREIGN_KEY_CHECKS=0;\n"]
lines.append("\n-- 学习资源（200 条，resource_no 与 Neo4j resource_id 严格一致）\n")
lines.append("INSERT INTO learning_resource "
             "(id, resource_no, title, category_id, college_id, major_id, course_id, primary_kp_id, "
             "type, file_url, cover_url, description, author, uploader_id, uploader_type, "
             "view_count, download_count, favorite_count, avg_rating, rating_count, heat, status, audit_time, create_time, update_time) VALUES\n")
res_rows = []
for r in resources:
    res_rows.append(
        f"({r['id']}, {S(r['resource_no'])}, {S(r['title'])}, {r['category_id']}, {r['college_id']}, {r['major_id']}, "
        f"{r['course_id']}, {r['primary_kp_id'] if r['primary_kp_id'] else 'NULL'}, {r['type']}, {S(r['file_url'])}, "
        f"{S(r['cover_url'])}, {S(r['description'])}, {S(r['author'])}, {r['uploader_id']}, {r['uploader_type']}, "
        f"{r['view_count']}, {r['download_count']}, {r['favorite_count']}, {r['avg_rating']}, {r['rating_count']}, "
        f"{r['heat']}, {r['status']}, {S(r['audit_time']) if r['audit_time'] else 'NULL'}, {S(r['create_time'])}, {S(r['update_time'])})"
    )
lines.append(",\n".join(res_rows) + ";\n")

lines.append("\n-- 资源审核记录\nINSERT INTO resource_review_record "
             "(id, resource_id, submitter_id, reviewer_id, action, review_comment, create_time) VALUES\n")
lines.append(",\n".join(
    f"({rr['id']}, {rr['resource_id']}, {rr['submitter_id'] if rr['submitter_id'] else 'NULL'}, "
    f"{rr['reviewer_id'] if rr['reviewer_id'] else 'NULL'}, {rr['action']}, {S(rr['review_comment'])}, {S(rr['create_time'])})"
    for rr in review_records
) + ";\n")

lines.append("\n-- Neo4j 同步日志\nINSERT INTO neo4j_sync_log "
             "(id, resource_id, sync_type, sync_status, error_msg, create_time) VALUES\n")
lines.append(",\n".join(
    f"({sl['id']}, {sl['resource_id']}, {sl['sync_type']}, {sl['sync_status']}, {S(sl['error_msg']) if sl['error_msg'] else 'NULL'}, {S(sl['create_time'])})"
    for sl in sync_logs
) + ";\n")
lines.append("SET FOREIGN_KEY_CHECKS=1;\n")
W("04_resource.sql", lines)

# ============================================================
# 05_user.sql  (管理员 + 学生)
# ============================================================
# 生成 BCrypt 密码
import bcrypt
stu_pwd = bcrypt.hashpw("123456".encode(), bcrypt.gensalt(rounds=10)).decode()
adm_pwd = bcrypt.hashpw("admin123".encode(), bcrypt.gensalt(rounds=10)).decode()

lines = ["-- 05_user.sql  管理员 + 学生用户\n", "SET FOREIGN_KEY_CHECKS=0;\n"]
lines.append("\n-- 管理员（admin/admin123、manager/admin123，密码为 BCrypt 加密）\n")
lines.append("INSERT INTO admin (id, username, password, real_name, role, email, phone, status, last_login_time, create_time, update_time) VALUES\n")
lines.append(",\n".join(
    f"({a['id']}, {S(a['username'])}, {S(adm_pwd)}, {S(a['real_name'])}, {a['role']}, {S(a['email'])}, {S(a['phone'])}, 1, NULL, {S(rand_dt('2025-01-01 08:00:00','2025-02-01 08:00:00'))}, {S(rand_dt('2025-01-01 08:00:00','2025-02-01 08:00:00'))})"
    for a in admins
) + ";\n")
lines.append("\n-- 学生用户（密码统一为 BCrypt('123456')，可直接登录）\n")
lines.append("INSERT INTO student_user "
             "(id, student_no, username, password, nickname, real_name, gender, college_id, major_id, grade, email, phone, avatar, status, create_time, update_time) VALUES\n")
lines.append(",\n".join(
    f"({s['id']}, {S(s['student_no'])}, {S(s['username'])}, {S(stu_pwd)}, {S(s['nickname'])}, {S(s['real_name'])}, "
    f"{s['gender']}, {s['college_id']}, {s['major_id']}, {S(s['grade'])}, {S(s['email'])}, {S(s['phone'])}, "
    f"{S(s['avatar'])}, {s['status']}, {S(s['create_time'])}, {S(s['create_time'])})"
    for s in students
) + ";\n")
lines.append("SET FOREIGN_KEY_CHECKS=1;\n")
W("05_user.sql", lines)

# ============================================================
# 06_behavior.sql  (收藏 / 下载 / 评分)
# ============================================================
lines = ["-- 06_behavior.sql  学生行为（ItemCF 数据源：收藏/下载/评分）\n", "SET FOREIGN_KEY_CHECKS=0;\n"]

fav_list = sorted(fav, key=lambda x: (x[0], x[1]))
lines.append("\n-- 收藏行为\nINSERT INTO favorite_behavior (id, student_id, resource_id, create_time) VALUES\n")
lines.append(",\n".join(
    f"({i}, {s}, {r}, {S(rand_dt('2025-10-01 08:00:00','2026-06-12 22:00:00'))})"
    for i, (s, r) in enumerate(fav_list, start=1)
) + ";\n")

dl_list = sorted(dl, key=lambda x: (x[0], x[1]))
lines.append("\n-- 下载行为\nINSERT INTO download_behavior (id, student_id, resource_id, create_time) VALUES\n")
lines.append(",\n".join(
    f"({i}, {s}, {r}, {S(rand_dt('2025-10-01 08:00:00','2026-06-12 22:00:00'))})"
    for i, (s, r) in enumerate(dl_list, start=1)
) + ";\n")

rate_list = sorted(rate.items(), key=lambda kv: (kv[0][0], kv[0][1]))
lines.append("\n-- 评分行为\nINSERT INTO rating_behavior (id, student_id, resource_id, score, create_time, update_time) VALUES\n")
lines.append(",\n".join(
    f"({i}, {s}, {r}, {sc}, {S(rand_dt('2025-10-01 08:00:00','2026-06-12 22:00:00'))}, {S(rand_dt('2025-10-01 08:00:00','2026-06-12 22:00:00'))})"
    for i, ((s, r), sc) in enumerate(rate_list, start=1)
) + ";\n")
lines.append("SET FOREIGN_KEY_CHECKS=1;\n")
W("06_behavior.sql", lines)

# ============================================================
# 07_social.sql  (评论 / 需求 / 问答 / 公告)
# ============================================================
lines = ["-- 07_social.sql  评论 / 资源需求 / 问答 / 系统公告\n", "SET FOREIGN_KEY_CHECKS=0;\n"]

lines.append("\n-- 资源评论\nINSERT INTO resource_comment (id, resource_id, student_id, content, parent_id, like_count, create_time) VALUES\n")
lines.append(",\n".join(
    f"({c['id']}, {c['resource_id']}, {c['student_id']}, {S(c['content'])}, {c['parent_id'] if c['parent_id'] else 'NULL'}, {c['like_count']}, {S(c['create_time'])})"
    for c in comments
) + ";\n") if comments else lines.append("\n-- 资源评论（空）\n")

lines.append("\n-- 资源需求\nINSERT INTO resource_requirement (id, student_id, title, content, category_id, college_id, status, reply_count, create_time, update_time) VALUES\n")
lines.append(",\n".join(
    f"({rq['id']}, {rq['student_id']}, {S(rq['title'])}, {S(rq['content'])}, {rq['category_id']}, {rq['college_id']}, {rq['status']}, {rq['reply_count']}, {S(rq['create_time'])}, {S(rq['create_time'])})"
    for rq in requirements
) + ";\n")

lines.append("\n-- 需求回复\nINSERT INTO requirement_reply (id, requirement_id, student_id, content, like_count, create_time) VALUES\n")
lines.append(",\n".join(
    f"({rr['id']}, {rr['requirement_id']}, {rr['student_id']}, {S(rr['content'])}, {rr['like_count']}, {S(rr['create_time'])})"
    for rr in req_replies
) + ";\n") if req_replies else lines.append("\n-- 需求回复（空）\n")

lines.append("\n-- 问答帖子\nINSERT INTO qa_post (id, student_id, title, content, category_id, tags, view_count, reply_count, like_count, status, create_time, update_time) VALUES\n")
lines.append(",\n".join(
    f"({p['id']}, {p['student_id']}, {S(p['title'])}, {S(p['content'])}, {p['category_id']}, {S(p['tags'])}, {p['view_count']}, {p['reply_count']}, {p['like_count']}, {p['status']}, {S(p['create_time'])}, {S(p['create_time'])})"
    for p in qa_posts
) + ";\n")

lines.append("\n-- 问答回复\nINSERT INTO qa_reply (id, post_id, student_id, content, parent_id, like_count, is_best, create_time) VALUES\n")
lines.append(",\n".join(
    f"({r['id']}, {r['post_id']}, {r['student_id']}, {S(r['content'])}, {r['parent_id'] if r['parent_id'] else 'NULL'}, {r['like_count']}, {r['is_best']}, {S(r['create_time'])})"
    for r in qa_replies
) + ";\n")

lines.append("\n-- 系统公告\nINSERT INTO system_announcement (id, admin_id, title, content, priority, publish_status, create_time, publish_time) VALUES\n")
lines.append(",\n".join(
    f"({a['id']}, {a['admin_id']}, {S(a['title'])}, {S(a['content'])}, {a['priority']}, {a['publish_status']}, {S(a['create_time'])}, {S(a['publish_time']) if a['publish_time'] else 'NULL'})"
    for a in ann_list
) + ";\n")
lines.append("SET FOREIGN_KEY_CHECKS=1;\n")
W("07_social.sql", lines)

# ============================================================
# neo4j_init.cypher
# ============================================================
clines = ["// ============================================================\n",
          "// neo4j_init.cypher  知识图谱初始化（严格依据 02-知识图谱设计.md）\n",
          "// 与 MySQL 关联键：Resource.resource_id == learning_resource.resource_no\n",
          "// 执行方式：Neo4j Browser 粘贴执行，或用 cypher-shell / driver 批量执行\n",
          "// ============================================================\n\n",
          "// 4.1 约束与索引\n"]
clines.append("CREATE CONSTRAINT college_id_unique IF NOT EXISTS FOR (c:College) REQUIRE c.id IS UNIQUE;\n")
clines.append("CREATE CONSTRAINT major_id_unique IF NOT EXISTS FOR (m:Major) REQUIRE m.id IS UNIQUE;\n")
clines.append("CREATE CONSTRAINT course_id_unique IF NOT EXISTS FOR (c:Course) REQUIRE c.id IS UNIQUE;\n")
clines.append("CREATE CONSTRAINT kp_id_unique IF NOT EXISTS FOR (k:KnowledgePoint) REQUIRE k.id IS UNIQUE;\n")
clines.append("CREATE CONSTRAINT resource_id_unique IF NOT EXISTS FOR (r:Resource) REQUIRE r.resource_id IS UNIQUE;\n")
clines.append("CREATE INDEX kp_name_idx IF NOT EXISTS FOR (k:KnowledgePoint) ON (k.name);\n")
clines.append("CREATE INDEX kp_heat_idx IF NOT EXISTS FOR (k:KnowledgePoint) ON (k.heat);\n")
clines.append("CREATE INDEX resource_heat_idx IF NOT EXISTS FOR (r:Resource) ON (r.heat);\n\n")

# 节点
clines.append("// ---- College 节点 ----\n")
for (cid, name, code, desc) in colleges:
    clines.append(f"MERGE (c:College {{id:{cid}, name:{C(name)}, code:{C(code)}, description:{C(desc)}}});\n")
clines.append("\n// ---- Major 节点 ----\n")
for (mid, col, name, code, desc) in majors:
    clines.append(f"MERGE (m:Major {{id:{mid}, college_id:{col}, name:{C(name)}, code:{C(code)}, description:{C(desc)}}});\n")
clines.append("\n// ---- Course 节点 ----\n")
for (cid, col, maj, name, code, teacher, credit, sem) in courses:
    clines.append(f"MERGE (co:Course {{id:{cid}, college_id:{col}, major_id:{maj}, name:{C(name)}, code:{C(code)}, teacher:{C(teacher)}, credit:{float(credit)}, semester:{C(sem)}}});\n")
clines.append("\n// ---- KnowledgePoint 节点 ----\n")
for k in knowledge_points:
    clines.append(f"MERGE (kp:KnowledgePoint {{id:{k['id']}, name:{C(k['name'])}, description:{C(k['desc'])}, course_id:{k['course_id']}, heat:{float(k['heat'])}}});\n")
clines.append("\n// ---- Resource 节点（resource_id 与 MySQL resource_no 一致） ----\n")
for r in resources:
    clines.append(f"MERGE (re:Resource {{resource_id:{C(r['resource_no'])}, title:{C(r['title'])}, type:{r['type']}, author:{C(r['author'])}, view_count:{r['view_count']}, download_count:{r['download_count']}, avg_rating:{float(r['avg_rating'])}, heat:{float(r['heat'])}, status:{r['status']}}});\n")

# 关系
clines.append("\n// ---- BELONGS_TO: Major->College, Course->Major ----\n")
for (mid, col, name, code, desc) in majors:
    clines.append(f"MATCH (m:Major {{id:{mid}}}), (c:College {{id:{col}}}) MERGE (m)-[:BELONGS_TO]->(c);\n")
for (cid, col, maj, name, code, teacher, credit, sem) in courses:
    clines.append(f"MATCH (co:Course {{id:{cid}}}), (m:Major {{id:{maj}}}) MERGE (co)-[:BELONGS_TO]->(m);\n")

clines.append("\n// ---- PREREQUISITE: Course->Course ----\n")
for (f, t) in gen.course_prereq:
    clines.append(f"MATCH (a:Course {{id:{f}}}), (b:Course {{id:{t}}}) MERGE (a)-[:PREREQUISITE]->(b);\n")

clines.append("\n// ---- TEACHES: Course->KnowledgePoint ----\n")
for k in knowledge_points:
    clines.append(f"MATCH (co:Course {{id:{k['course_id']}}}), (kp:KnowledgePoint {{id:{k['id']}}}) MERGE (co)-[:TEACHES]->(kp);\n")

clines.append("\n// ---- PREREQUISITE / RELATED_TO / CONTAINS: KnowledgePoint->KnowledgePoint ----\n")
for (a, b, t) in kp_rels:
    clines.append(f"MATCH (x:KnowledgePoint {{id:{a}}}), (y:KnowledgePoint {{id:{b}}}) MERGE (x)-[:{t}]->(y);\n")

clines.append("\n// ---- BELONGS_TO_KP: Resource->KnowledgePoint ----\n")
for r in resources:
    if r["primary_kp_id"]:
        clines.append(f"MATCH (re:Resource {{resource_id:{C(r['resource_no'])}}}), (kp:KnowledgePoint {{id:{r['primary_kp_id']}}}) MERGE (re)-[:BELONGS_TO_KP]->(kp);\n")

W("neo4j_init.cypher", clines)

# ============================================================
# README.md
# ============================================================
passed = sum(1 for r in resources if r["status"] == 1)
pending = sum(1 for r in resources if r["status"] == 0)
rejected = sum(1 for r in resources if r["status"] == 2)
# 统计边
edge_course_belongs = len(majors) + len(courses)
edge_course_pre = len(gen.course_prereq)
edge_teaches = len(knowledge_points)
edge_kp = len(kp_rels)
edge_belongs_kp = sum(1 for r in resources if r["primary_kp_id"])
total_edges = edge_course_belongs + edge_course_pre + edge_teaches + edge_kp + edge_belongs_kp

readme = f"""# 模拟种子数据说明（毕业设计第六套）

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
| college | {len(colleges)} | learning_resource | {len(resources)}（通过 {passed} / 待审 {pending} / 驳回 {rejected}） |
| major | {len(majors)} | resource_comment | {len(comments)} |
| course | {len(courses)} | favorite_behavior | {len(fav)} |
| resource_category | {len(categories)} | download_behavior | {len(dl)} |
| student_user | {len(students)} | rating_behavior | {len(rate)} |
| admin | {len(admins)} | resource_requirement | {len(requirements)} |
| | | requirement_reply | {len(req_replies)} |
| | | qa_post | {len(qa_posts)} |
| | | qa_reply | {len(qa_replies)} |
| | | system_announcement | {len(ann_list)} |
| | | resource_review_record | {len(review_records)} |
| | | neo4j_sync_log | {len(sync_logs)} |

### Neo4j 图谱规模
| 节点标签 | 数量 | 关系类型 | 数量 |
|---------|-----|---------|-----|
| College | {len(colleges)} | BELONGS_TO（Major→College, Course→Major） | {edge_course_belongs} |
| Major | {len(majors)} | PREREQUISITE（Course→Course） | {edge_course_pre} |
| Course | {len(courses)} | TEACHES（Course→KnowledgePoint） | {edge_teaches} |
| KnowledgePoint | {len(knowledge_points)} | PREREQUISITE/RELATED_TO/CONTAINS（KP→KP） | {edge_kp} |
| Resource | {len(resources)} | BELONGS_TO_KP（Resource→KnowledgePoint） | {edge_belongs_kp} |
| **节点合计** | **{len(colleges)+len(majors)+len(courses)+len(knowledge_points)+len(resources)}** | **边合计** | **{total_edges}** |

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
"""
W("README.md", readme)
print("ALL FILES WRITTEN.")
