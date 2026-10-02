package com.campus.recommend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结构 {code, message, data}
 * code: 0=成功；非0=失败（1001参数错误，1002未登录/无权限，1003资源不存在，5000服务器错误）
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务码 */
    private int code;
    /** 提示文案 */
    private String message;
    /** 业务数据 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（带数据） */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功（无数据） */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 失败（指定码与文案） */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /** 失败（使用 ResultCode 枚举） */
    public static <T> Result<T> error(ResultCode rc) {
        return new Result<>(rc.getCode(), rc.getMessage(), null);
    }

    /** 失败（使用 ResultCode 枚举 + 自定义文案） */
    public static <T> Result<T> error(ResultCode rc, String message) {
        return new Result<>(rc.getCode(), message, null);
    }
}
