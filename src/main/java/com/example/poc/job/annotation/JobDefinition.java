package com.example.poc.job.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface JobDefinition {
    String name();
    String group() default "DEFAULT";
    String description() default "";
}
