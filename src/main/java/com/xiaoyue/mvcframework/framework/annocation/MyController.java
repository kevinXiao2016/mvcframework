package com.xiaoyue.mvcframework.framework.annocation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyController {

    String value() default "";


}
