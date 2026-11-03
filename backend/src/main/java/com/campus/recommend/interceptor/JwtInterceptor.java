package com.campus.recommend.interceptor;

import com.campus.recommend.annotation.RequireRole;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.context.LoginUser;
import com.campus.recommend.util.JwtUtil;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.mapper.AdminMapper;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.entity.Admin;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT 拦截器：
 * 1. 公开接口直接放行；
 * 2. 其余接口校验 Authorization: Bearer <token>；
 * 3. 解析出的用户信息写入 LoginContext；
 * 4. 若方法/类标注 @RequireRole，校验角色。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired private StudentUserMapper students;
    @Autowired private AdminMapper admins;

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 只拦截带 @RequestMapping 的方法
            if (!(handler instanceof HandlerMethod)) {
                return true;
            }
            String method = request.getMethod();
            String path = request.getRequestURI();

            // 1. 公开接口放行
            if (isPublic(method, path)) {
                return true;
            }

            // 2. 解析 token
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "缺少有效的 Authorization 头");
            }
            String token = auth.substring(7).trim();
            Claims claims = jwtUtil.parseToken(token);

            Long uid;
            String role;
            String username;
            try {
                uid = jwtUtil.getUserId(claims);
                role = jwtUtil.getRole(claims);
                username = jwtUtil.getUsername(claims);
            } catch (RuntimeException e) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "token 身份无效");
            }
            if (uid == null || username == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "token 身份无效");
            String currentPassword;
            if ("STUDENT".equals(role)) {
                StudentUser student = students.selectById(uid);
                if (student == null || !Integer.valueOf(1).equals(student.getStatus()) || !username.equals(student.getUsername()))
                    throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不可用");
                currentPassword = student.getPassword();
            } else if ("ADMIN".equals(role)) {
                Admin admin = admins.selectById(uid);
                if (admin == null || !Integer.valueOf(1).equals(admin.getStatus()) || !username.equals(admin.getUsername()))
                    throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不可用");
                currentPassword = admin.getPassword();
            } else throw new BusinessException(ResultCode.UNAUTHORIZED, "token 角色无效");
            String tokenVersion = claims.get("pv", String.class);
            String currentVersion = jwtUtil.passwordVersion(currentPassword);
            if (tokenVersion == null || currentVersion == null || !java.security.MessageDigest.isEqual(
                    tokenVersion.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    currentVersion.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");

            LoginUser lu = new LoginUser();
            lu.setUserId(jwtUtil.getUserId(claims));
            lu.setRole(jwtUtil.getRole(claims));
            lu.setUsername(jwtUtil.getUsername(claims));
            LoginContext.set(lu);

            // 3. 角色校验
            HandlerMethod hm = (HandlerMethod) handler;
            RequireRole roleAnno = hm.getMethodAnnotation(RequireRole.class);
            if (roleAnno == null) {
                roleAnno = hm.getBeanType().getAnnotation(RequireRole.class);
            }
            if (roleAnno != null) {
                Set<String> allowed = new HashSet<>(Arrays.asList(roleAnno.value()));
                if (!allowed.contains(lu.getRole())) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED, "权限不足，需要角色：" + allowed);
                }
            }
            return true;
        } catch (RuntimeException e) {
            LoginContext.clear();
            throw e;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginContext.clear();
    }

    /** 判断是否为公开接口（无需登录） */
    private boolean isPublic(String method, String path) {
        // 鉴权类公开
        if ("POST".equals(method)) {
            if (path.equals("/api/auth/login")
                    || path.equals("/api/auth/register")
                    || path.equals("/api/auth/forgot-password")
                    || path.equals("/api/auth/reset-password")) {
                return true;
            }
        }
        if ("GET".equals(method)) {
            // 公开资源检索：列表检索 + 资源详情（/api/resource/{id} 精确）
            if (path.equals("/api/resource/search")) {
                return true;
            }
            if (path.matches("^/api/resource/\\d+$")) {
                return true;
            }
            // 知识图谱可视化公开
            if (path.startsWith("/api/graph/")) {
                return true;
            }
            // 热门推荐 / 语义相关推荐 公开
            if (path.equals("/api/recommend/hot") || path.equals("/api/recommend/related")) {
                return true;
            }
        }
        return false;
    }
}
