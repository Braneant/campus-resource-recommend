package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 图谱新增/删除关系入参 */
@Data
public class GraphRelationDTO {
    @NotBlank(message = "起始节点类型不能为空")
    private String fromType;
    @NotNull(message = "起始节点id不能为空")
    private String fromId;
    @NotBlank(message = "目标节点类型不能为空")
    private String toType;
    @NotNull(message = "目标节点id不能为空")
    private String toId;
    @NotBlank(message = "关系类型不能为空")
    private String relType;
}
