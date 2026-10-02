package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 发表评论入参 */
@Data
public class CommentDTO {
    @NotBlank(message = "评论内容不能为空")
    private String content;
    /** 楼中楼父评论 id */
    private Long parentId;
}
