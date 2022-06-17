package com.le.yygh.common.utils;

import com.le.yygh.common.helper.JwtHelper;

import javax.servlet.http.HttpServletRequest;

//获取当前用户信息工具类（从请求头中的token中获取，因为登录时放进去了）
public class AuthContextHolder {

    //获取当前用户id
    public static Long getUserId(HttpServletRequest request) {
        //从header获取token
        String token = request.getHeader("token");
        //jwt从token获取userid
        Long userId = JwtHelper.getUserId(token);
        return userId;
    }

    //获取当前用户名称
    public static String getUserName(HttpServletRequest request) {
        //从header获取token
        String token = request.getHeader("token");
        //jwt从token获取userName
        String userName = JwtHelper.getUserName(token);
        return userName;
    }
}
