package com.campus.recommend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 评分入参 */
@Data
public class RatingDTO {
    @NotNull(message = "评分不能为空")
    @Min(1)
    @Max(5)
    private Integer score;
}
