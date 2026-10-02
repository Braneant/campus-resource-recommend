package com.campus.recommend.common;

import lombok.Getter;

/**
 * 业务码枚举（与 03-接口契约.md §1.1 保持一致）
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(1001, "参数错误"),
    UNAUTHORIZED(1002, "未登录或无权限"),
    NOT_FOUND(1003, "资源不存在"),
    SERVER_ERROR(5000, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
