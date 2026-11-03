package com.campus.recommend.controller;

import com.campus.recommend.common.Result;
import com.campus.recommend.dto.LoginDTO;
import com.campus.recommend.dto.RegisterDTO;
import com.campus.recommend.dto.ForgotPasswordDTO;
import com.campus.recommend.dto.ResetPasswordDTO;
import com.campus.recommend.service.PasswordResetService;
import com.campus.recommend.service.AuthService;
import com.campus.recommend.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 鉴权模块 /api/auth */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired private PasswordResetService passwordResetService;

    /** 学生注册 */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /** 登录（学生/管理员统一入口） */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto.getUsername(), dto.getPassword()));
    }

    /** 找回密码（校验学号 + 邮箱） */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO body) {
        passwordResetService.request(body.getStudentNo(), body.getEmail());
        return Result.success();
    }

    /** 重置密码 */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO body) {
        passwordResetService.reset(body.getUsername(), body.getCode(), body.getNewPassword());
        return Result.success();
    }
}
