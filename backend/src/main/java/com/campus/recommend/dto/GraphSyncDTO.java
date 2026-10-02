package com.campus.recommend.dto;

import lombok.Data;

/** 图谱全量同步入参 */
@Data
public class GraphSyncDTO {
    /** 同步范围：all / resource / college */
    private String scope = "all";
}
