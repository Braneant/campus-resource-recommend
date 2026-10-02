package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 问答回复入参 */
@Data
public class QaReplyDTO {
    @NotBlank(message = "回复内容不能为空")
    private String content;
    private Long parentId;
}
