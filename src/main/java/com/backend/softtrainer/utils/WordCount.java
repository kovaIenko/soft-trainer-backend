package com.backend.softtrainer.utils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = WordCountValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WordCount {
    String message() default "Text exceeds maximum word count";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    int max() default Integer.MAX_VALUE;
    int min() default 0;
} 