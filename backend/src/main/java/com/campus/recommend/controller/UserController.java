package com.campus.recommend.controller;

import com.campus.recommend.common.Result;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.dto.ProfileUpdateDTO;
import com.campus.recommend.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 用户模块 /api/user */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /** 个人资料 */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        return Result.success(userService.getProfile());
    }

    /** 更新资料 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return Result.success();
    }
}
