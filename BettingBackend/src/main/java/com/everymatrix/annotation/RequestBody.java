package com.everymatrix.annotation;
import com.everymatrix.converter.ParamParser;
import com.everymatrix.converter.StringParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {
    Class<? extends ParamParser> converter() default StringParser.class;
}
