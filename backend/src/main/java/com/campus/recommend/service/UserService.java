package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.dto.ProfileUpdateDTO;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.mapper.StudentUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/** 用户服务：个人资料 / 修改资料 / 修改密码 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private StudentUserMapper studentUserMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 获取个人资料（密码脱敏） */
    public Map<String, Object> getProfile() {
        Long stuId = LoginContext.getStudentId();
        if (stuId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        StudentUser stu = studentUserMapper.selectById(stuId);
        if (stu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toSafeMap(stu);
    }

    /** 更新个人资料 */
    public void updateProfile(ProfileUpdateDTO dto) {
        Long stuId = LoginContext.getStudentId();
        StudentUser stu = studentUserMapper.selectById(stuId);
        if (stu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(dto.getNickname())) stu.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getRealName())) stu.setRealName(dto.getRealName());
        if (dto.getGender() != null) stu.setGender(dto.getGender());
        if (StringUtils.hasText(dto.getEmail())) stu.setEmail(dto.getEmail());
        if (StringUtils.hasText(dto.getPhone())) stu.setPhone(dto.getPhone());
        if (StringUtils.hasText(dto.getAvatar())) stu.setAvatar(dto.getAvatar());
        if (dto.getCollegeId() != null) stu.setCollegeId(dto.getCollegeId());
        if (dto.getMajorId() != null) stu.setMajorId(dto.getMajorId());
        studentUserMapper.updateById(stu);
    }

    /** 修改密码 */
    public void changePassword(String oldPassword, String newPassword) {
        Long stuId = LoginContext.getStudentId();
        StudentUser stu = studentUserMapper.selectById(stuId);
        if (stu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, stu.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "原密码错误");
        }
        stu.setPassword(passwordEncoder.encode(newPassword));
        studentUserMapper.updateById(stu);
    }

    private Map<String, Object> toSafeMap(StudentUser stu) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", stu.getId());
        m.put("studentNo", stu.getStudentNo());
        m.put("username", stu.getUsername());
        m.put("nickname", stu.getNickname());
        m.put("realName", stu.getRealName());
        m.put("gender", stu.getGender());
        m.put("collegeId", stu.getCollegeId());
        m.put("majorId", stu.getMajorId());
        m.put("grade", stu.getGrade());
        m.put("email", stu.getEmail());
        m.put("phone", stu.getPhone());
        m.put("avatar", stu.getAvatar());
        m.put("status", stu.getStatus());
        return m;
    }
}
