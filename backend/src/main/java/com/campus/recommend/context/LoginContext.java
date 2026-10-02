package com.campus.recommend.context;

/**
 * 登录用户上下文（ThreadLocal 持有 LoginUser）
 */
public class LoginContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前学生id（非学生场景返回 null） */
    public static Long getStudentId() {
        LoginUser u = HOLDER.get();
        if (u == null) return null;
        return LoginUser.ROLE_STUDENT.equals(u.getRole()) ? u.getUserId() : null;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
