package com.campus.recommend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色约束注解：标注在 Controller 类或方法上，要求调用者具备指定角色之一。
 * 例如 @RequireRole("ADMIN") 仅允许管理员访问。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
}
