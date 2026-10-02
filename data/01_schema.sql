-- ============================================================
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
