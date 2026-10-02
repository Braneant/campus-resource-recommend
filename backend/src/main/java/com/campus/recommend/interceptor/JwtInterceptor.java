package com.campus.recommend.interceptor;

import com.campus.recommend.annotation.RequireRole;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.context.LoginUser;
import com.campus.recommend.util.JwtUtil;
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
        } finally {
            // 注意：请求结束后由 afterCompletion 清除；异常时也要清除
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
