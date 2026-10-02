package com.campus.recommend.controller;

import com.campus.recommend.annotation.RequireRole;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.Result;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.service.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 管理端用户权限管控（前端 /api/admin/user 约定扩展接口） */
@Slf4j
@RestController
@RequestMapping("/api/admin/user")
@RequireRole("ADMIN")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /** 分页列表 */
    @GetMapping
    public Result<PageResult<StudentUser>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminUserService.list(pageNum, pageSize, keyword));
    }

    /** 新增用户 */
    @PostMapping
    public Result<Void> create(@RequestBody StudentUser u) {
        adminUserService.create(u);
        return Result.success();
    }

    /** 更新用户 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody StudentUser u) {
        adminUserService.update(id, u);
        return Result.success();
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Result.success();
    }

    /** 设置角色（占位兼容） */
    @PutMapping("/{id}/role")
    public Result<Void> setRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminUserService.setRole(id, body.get("role"));
        return Result.success();
    }

    /** 启用 / 停用 */
    @PutMapping("/{id}/enable")
    public Result<Void> setEnable(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminUserService.setEnable(id, body.get("status"));
        return Result.success();
    }
}
