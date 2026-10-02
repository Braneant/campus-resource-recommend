package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 学生上传资源入参（file 单独用 @RequestParam 接收） */
@Data
public class ResourceUploadDTO {
    @NotBlank(message = "资源标题不能为空")
    private String title;
    @NotNull(message = "资源分类不能为空")
    private Long categoryId;
    private Long collegeId;
    private Long majorId;
    private Long courseId;
    /** 主知识点 id（关联 Neo4j KnowledgePoint.id） */
    private Long kpId;
    /** 资源类型：1文档 2视频 3课件 4其他 */
    private Integer type = 1;
    private String description;
    private String author;
}
