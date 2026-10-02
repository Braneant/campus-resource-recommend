package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.mapper.StudentUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/** 管理端用户权限管控服务：学生用户列表 / 新增 / 更新 / 删除 / 角色 / 状态 */
@Slf4j
@Service
public class AdminUserService {

    @Autowired
    private StudentUserMapper studentUserMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 分页列表（支持关键词检索） */
    public PageResult<StudentUser> list(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<StudentUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(StudentUser::getUsername, keyword)
                    .or().like(StudentUser::getNickname, keyword)
                    .or().like(StudentUser::getStudentNo, keyword)
                    .or().like(StudentUser::getRealName, keyword));
        }
        qw.orderByDesc(StudentUser::getCreateTime);
        Page<StudentUser> page = studentUserMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        // 密码脱敏，避免返回密文
        page.getRecords().forEach(u -> u.setPassword(null));
        return new PageResult<>(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    /** 新增用户（默认学生角色、正常状态，密码默认 123456） */
    public void create(StudentUser u) {
        if (!StringUtils.hasText(u.getUsername())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名不能为空");
        }
        Long count = studentUserMapper.selectCount(
                new LambdaQueryWrapper<StudentUser>().eq(StudentUser::getUsername, u.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已存在");
        }
        if (!StringUtils.hasText(u.getPassword())) {
            u.setPassword("123456");
        }
        u.setPassword(passwordEncoder.encode(u.getPassword()));
        if (u.getStatus() == null) u.setStatus(1);
        studentUserMapper.insert(u);
    }

    /** 更新用户（资料 + 状态，不含密码；密码由 resetPassword 单独处理） */
    public void update(Long id, StudentUser u) {
        StudentUser exist = studentUserMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(u.getNickname())) exist.setNickname(u.getNickname());
        if (StringUtils.hasText(u.getRealName())) exist.setRealName(u.getRealName());
        if (u.getGender() != null) exist.setGender(u.getGender());
        if (StringUtils.hasText(u.getEmail())) exist.setEmail(u.getEmail());
        if (StringUtils.hasText(u.getPhone())) exist.setPhone(u.getPhone());
        if (u.getCollegeId() != null) exist.setCollegeId(u.getCollegeId());
        if (u.getMajorId() != null) exist.setMajorId(u.getMajorId());
        if (StringUtils.hasText(u.getGrade())) exist.setGrade(u.getGrade());
        if (u.getStatus() != null) exist.setStatus(u.getStatus());
        studentUserMapper.updateById(exist);
    }

    /** 删除用户（软删除：置状态 2=已删除；保留行为数据供推荐回溯） */
    public void delete(Long id) {
        StudentUser exist = studentUserMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        exist.setStatus(2);
        studentUserMapper.updateById(exist);
    }

    /** 重置角色（本系统两角色：STUDENT / ADMIN，此处仅支持学生角色管控，占位兼容） */
    public void setRole(Long id, String role) {
        StudentUser exist = studentUserMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // student_user 表无 role 字段，角色由登录类型决定；此处仅做状态校验，role 参数预留
        log.info("setRole userId={}, role={}（本系统学生/管理员角色分离，学生角色固定）", id, role);
    }

    /** 启用 / 停用（状态切换） */
    public void setEnable(Long id, Integer status) {
        StudentUser exist = studentUserMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        exist.setStatus(status);
        studentUserMapper.updateById(exist);
    }
}
