package com.le.yygh.common.exception;

import com.le.yygh.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result error(Exception e) {
        e.printStackTrace();
        return Result.fail();
    }

    @ExceptionHandler(YyghException.class)
    public Result error(YyghException e) {
        e.printStackTrace();
        //return Result.fail();//原来
        //System.out.println("获取的的e.getMessage为:"+e.getMessage());
        //return Result.fail(e.getMessage());
        return Result.fail(e);//5.26

    }
}
