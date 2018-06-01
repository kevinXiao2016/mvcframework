package com.xiaoyue.mvcframework.framework.annocation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAutowire {
    String value() default "";
}
