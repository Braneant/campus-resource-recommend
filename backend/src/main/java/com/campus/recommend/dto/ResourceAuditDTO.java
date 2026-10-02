package com.campus.recommend.dto;

import lombok.Data;

/** 资源审核入参（通过/驳回共用，按字段可选） */
@Data
public class ResourceAuditDTO {
    /** 审核通过时指定的主知识点 id（可选） */
    private Long kpId;
    /** 审核驳回时的审核意见 */
    private String reviewComment;
}
