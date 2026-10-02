package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 发布资源需求入参 */
@Data
public class RequirementDTO {
    @NotBlank(message = "需求标题不能为空")
    private String title;
    @NotBlank(message = "需求内容不能为空")
    private String content;
    private Long categoryId;
    private Long collegeId;
}
