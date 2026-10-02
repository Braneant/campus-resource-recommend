package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.context.LoginUser;
import com.campus.recommend.dto.RegisterDTO;
import com.campus.recommend.entity.Admin;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.mapper.AdminMapper;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.util.JwtUtil;
import com.campus.recommend.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权服务：注册 / 登录（学生与管理员统一入口）/ 找回密码 / 重置密码
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private StudentUserMapper studentUserMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    /** 学生注册 */
    public Map<String, Object> register(RegisterDTO dto) {
        // 用户名 / 学号 唯一性校验
        if (studentUserMapper.selectCount(new LambdaQueryWrapper<StudentUser>()
                .eq(StudentUser::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已存在");
        }
        if (studentUserMapper.selectCount(new LambdaQueryWrapper<StudentUser>()
                .eq(StudentUser::getStudentNo, dto.getStudentNo())) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学号已存在");
        }
        StudentUser user = new StudentUser();
        user.setStudentNo(dto.getStudentNo());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setCollegeId(dto.getCollegeId());
        user.setMajorId(dto.getMajorId());
        user.setEmail(dto.getEmail());
        user.setGender(0);
        user.setStatus(1);
        studentUserMapper.insert(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        return data;
    }

    /** 登录（学生/管理员统一） */
    public LoginVO login(String username, String password) {
        // 先查学生
        StudentUser stu = studentUserMapper.selectOne(
                new LambdaQueryWrapper<StudentUser>().eq(StudentUser::getUsername, username));
        if (stu != null) {
            if (stu.getStatus() != null && stu.getStatus() == 0) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "账号已被禁用");
            }
            if (!passwordEncoder.matches(password, stu.getPassword())) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
            }
            String token = jwtUtil.generateToken(stu.getId(), LoginUser.ROLE_STUDENT, stu.getUsername());
            LoginVO vo = new LoginVO();
            vo.setToken(token);
            vo.setRole(LoginUser.ROLE_STUDENT);
            vo.setUser(buildStudentMap(stu));
            return vo;
        }
        // 再查管理员
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
        if (admin != null) {
            if (admin.getStatus() != null && admin.getStatus() == 0) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "账号已被禁用");
            }
            if (!passwordEncoder.matches(password, admin.getPassword())) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
            }
            // 更新最后登录时间
            admin.setLastLoginTime(java.time.LocalDateTime.now());
            adminMapper.updateById(admin);

            String token = jwtUtil.generateToken(admin.getId(), LoginUser.ROLE_ADMIN, admin.getUsername());
            LoginVO vo = new LoginVO();
            vo.setToken(token);
            vo.setRole(LoginUser.ROLE_ADMIN);
            Map<String, Object> am = new HashMap<>();
            am.put("id", admin.getId());
            am.put("username", admin.getUsername());
            am.put("realName", admin.getRealName());
            am.put("role", admin.getRole());
            vo.setUser(am);
            return vo;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
    }

    /** 找回密码：校验学号 + 邮箱（简化：仅校验存在性，真实场景发验证码） */
    public void forgotPassword(String studentNo, String email) {
        StudentUser stu = studentUserMapper.selectOne(new LambdaQueryWrapper<StudentUser>()
                .eq(StudentUser::getStudentNo, studentNo));
        if (stu == null || !StringUtils.hasText(email) || !email.equals(stu.getEmail())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学号或邮箱不匹配");
        }
        // 真实环境：此处发送验证码邮件；演示直接返回成功
    }

    /** 重置密码（简化：忽略验证码，直接按用户名更新） */
    public void resetPassword(String username, String newPassword) {
        StudentUser stu = studentUserMapper.selectOne(new LambdaQueryWrapper<StudentUser>()
                .eq(StudentUser::getUsername, username));
        if (stu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        stu.setPassword(passwordEncoder.encode(newPassword));
        studentUserMapper.updateById(stu);
    }

    private Map<String, Object> buildStudentMap(StudentUser stu) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", stu.getId());
        m.put("username", stu.getUsername());
        m.put("nickname", stu.getNickname());
        m.put("collegeId", stu.getCollegeId());
        m.put("majorId", stu.getMajorId());
        m.put("avatar", stu.getAvatar());
        return m;
    }
}
