package com.atguigu.gmall.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)  // @Retention 指定注解的生命周期
public @interface LoginRequire {
    // true ：则表示需要登录，否则不需要登录！
    boolean autoRedirect() default true; //默认值 true

}
