package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/** 图谱新增节点入参 */
@Data
public class GraphNodeCreateDTO {
    /** 节点类型：COLLEGE/MAJOR/COURSE/KNOWLEDGE_POINT/RESOURCE */
    @NotBlank(message = "节点类型不能为空")
    private String type;
    /** 节点属性（id/name/code 等） */
    @NotNull(message = "节点属性不能为空")
    private Map<String, Object> properties;
}
