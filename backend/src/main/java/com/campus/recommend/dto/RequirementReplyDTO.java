package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 回复需求入参 */
@Data
public class RequirementReplyDTO {
    @NotBlank(message = "回复内容不能为空")
    private String content;
}
