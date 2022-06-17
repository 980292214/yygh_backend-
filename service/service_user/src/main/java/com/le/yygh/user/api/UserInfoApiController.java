package com.le.yygh.user.api;

import com.le.yygh.common.result.Result;
import com.le.yygh.common.utils.AuthContextHolder;
import com.le.yygh.model.user.UserInfo;
import com.le.yygh.user.service.UserInfoService;
import com.le.yygh.vo.user.LoginVo;
import com.le.yygh.vo.user.UserAuthVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    //用户手机号登录接口
    @PostMapping("login")
    public Result login(@RequestBody LoginVo loginVo) {
        Map<String,Object> info = userInfoService.loginUser(loginVo);
        return Result.ok(info);//返回用户名用来显示，通过token返回（放在map当中）
    }

    //用户认证接口 用户认证的操作就是往用户信息里继续添加字段，后续还需审核
    @PostMapping("auth/userAuth")
    public Result userAuth(@RequestBody UserAuthVo userAuthVo, HttpServletRequest request) {
        //传递两个参数，第一个参数用户id，第二个参数认证数据vo对象；
        userInfoService.userAuth(AuthContextHolder.getUserId(request),userAuthVo);
        return Result.ok();
    }

    //获取用户id信息接口 ；方便前端展示
    @GetMapping("auth/getUserInfo")
    public Result getUserInfo(HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        UserInfo userInfo = userInfoService.getById(userId);
        return Result.ok(userInfo);
    }
}
