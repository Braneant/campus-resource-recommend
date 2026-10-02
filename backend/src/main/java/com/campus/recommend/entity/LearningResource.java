package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习资源表 learning_resource（核心业务表）
 * 注意字段名与 01-数据库设计.md 严格一致，resource_no 与 Neo4j Resource.resource_id 对应。
 */
@Data
@TableName("learning_resource")
public class LearningResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String resourceNo;
    private String title;
    private Long categoryId;
    private Long collegeId;
    private Long majorId;
    private Long courseId;
    private Long primaryKpId;
    /** 资源类型：1文档 2视频 3课件 4其他 */
    private Integer type;
    private String fileUrl;
    private String coverUrl;
    private String description;
    private String author;
    private Long uploaderId;
    /** 上传者类型：1学生 2管理员 */
    private Integer uploaderType;
    private Integer viewCount;
    private Integer downloadCount;
    private Integer favoriteCount;
    /** 平均评分 0.00~5.00 */
    private BigDecimal avgRating;
    private Integer ratingCount;
    /** 综合热度（定时计算） */
    private BigDecimal heat;
    /** 状态：0待审核 1已通过 2已驳回 */
    private Integer status;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
