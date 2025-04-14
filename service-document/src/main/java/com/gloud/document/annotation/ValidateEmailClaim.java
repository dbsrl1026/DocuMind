package com.gloud.document.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 어노테이션 적용할 위치  ElementType.METHOD : 메서드 선언
@Retention(RetentionPolicy.RUNTIME) //@Retention : 컴파일러가 어노테이션을 다루는 방법을 기술, 어느 시점까지 영향을 미치는지를 결정
//RetentionPolicy.RUNTIME : 컴파일 이후 런타임 시기에도 JVM에 의해 참조가 가능(리플렉션)
public @interface ValidateEmailClaim {
    String paramName() default "email";
}