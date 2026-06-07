package com.zhixue.exception;

import com.zhixue.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e){

        e.printStackTrace();

        return Result.error("系统异常：" + e.getMessage());
    }

}