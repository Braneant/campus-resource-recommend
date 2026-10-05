package com.campus.recommend.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

/**
 * 全局异常处理器：所有异常统一收敛为 {code, message, data}
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：直接用其携带的码与信息 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常（@Valid / @Validated） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        StringBuilder sb = new StringBuilder();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            sb.append(fe.getField()).append(":").append(fe.getDefaultMessage()).append("; ");
        }
        String msg = sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "参数校验失败";
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 参数类型不匹配（如分页参数收到非数字）：返回友好提示而非 500 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String name = e.getName() == null ? "参数" : e.getName();
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "参数格式错误：" + name);
    }

    /** 缺少必填参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "缺少必填参数：" + e.getParameterName());
    }

    /** 请求体格式错误（JSON 无法解析） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "提交的内容格式有误，请检查后重试");
    }

    /** 请求方式不被支持（如用 GET 调用了 POST 接口） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "当前请求方式不被支持：" + e.getMethod());
    }

    /** 上传文件超过大小限制：明确告知原因，而不是抛“服务器内部错误” */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "文件过大，请压缩后重新上传");
    }

    /** 访问的接口不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        return Result.error(ResultCode.NOT_FOUND.getCode(), "请求的接口不存在");
    }

    /** 兜底异常：日志保留完整堆栈，同时回传一个短编号便于用户报障时定位 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e, HttpServletRequest request) {
        String traceId = Integer.toHexString(UUID.randomUUID().hashCode());
        log.error("未捕获异常 traceId={} uri={}", traceId, request.getRequestURI(), e);
        return Result.error(ResultCode.SERVER_ERROR.getCode(),
                "服务暂时不可用，请稍后重试（错误编号：" + traceId + "）");
    }
}
