package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 系统公告入参 */
@Data
public class AnnouncementDTO {
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    /** 优先级：1普通 2重要 3紧急 */
    private Integer priority = 1;
    /** 发布状态：0草稿 1已发布 */
    private Integer publishStatus = 0;
}
