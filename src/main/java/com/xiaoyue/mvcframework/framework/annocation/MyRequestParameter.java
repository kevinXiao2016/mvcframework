package com.xiaoyue.mvcframework.framework.annocation;


import java.lang.annotation.*;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRequestParameter {
    String value() default "";
}
