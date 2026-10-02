package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 发帖入参 */
@Data
public class QaDTO {
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "正文不能为空")
    private String content;
    private Long categoryId;
    private String tags;
}
