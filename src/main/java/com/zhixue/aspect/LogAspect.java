package com.zhixue.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

@Aspect
@Component
public class LogAspect {

    @Around("execution(* com.zhixue.controller.*.*(..))")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();

        ServletRequestAttributes attrs =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        HttpServletRequest request = attrs.getRequest();

        System.out.println("=================================");
        System.out.println("请求URL：" + request.getRequestURI());
        System.out.println("请求方法：" + request.getMethod());
        System.out.println("执行方法：" + joinPoint.getSignature());

        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();

        System.out.println("耗时：" + (end - start) + "ms");
        System.out.println("=================================");

        return result;
    }
}