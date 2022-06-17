package com.le.yygh.user.controller;

import com.le.yygh.common.result.Result;
import com.le.yygh.model.user.UserInfo;
import com.le.yygh.user.service.UserInfoService;
import com.le.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api("后台用户管理接口")
@RestController
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private UserInfoService userInfoService;

    //用户列表（条件查询带分页）
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit,
                       UserInfoQueryVo userInfoQueryVo) {
        Page<UserInfo> pageParam = new Page<>(page,limit);
        IPage<UserInfo> pageModel =
                userInfoService.selectPage(pageParam,userInfoQueryVo);
        return Result.ok(pageModel);
    }

    //用户锁定
    @GetMapping("lock/{userId}/{status}")
    public Result lock(@PathVariable Long userId,@PathVariable Integer status) {
        userInfoService.lock(userId,status);
        return Result.ok();
    }

    //用户详情,包括用户信息和就诊人列表
    @GetMapping("show/{userId}")
    public Result show(@PathVariable Long userId) {
        Map<String,Object> map = userInfoService.show(userId);
        return Result.ok(map);
    }

    //认证审批
    @GetMapping("approval/{userId}/{authStatus}")
    public Result approval(@PathVariable Long userId,@PathVariable Integer authStatus) {
        userInfoService.approval(userId,authStatus);
        return Result.ok();
    }

    /**新加的，后台前端模糊查询使用 5.3
     * 根据用户认证的姓名，模糊查询叫该用户的所用用户列表，用作远程调用
     * @param username
     * @return
     */
    @ApiOperation("根据认证用户姓名模糊查询用户列表")
    @GetMapping("findUserListByUserName/{username}")
    public List<UserInfo> findUserListByUserName(@PathVariable String username){
        List<UserInfo> userInfoList =  userInfoService.findUserListByUserName(username);
        return userInfoList;
    }

    /**新加的，后台前端模糊查询使用 5.3
     * 根据用户id返回用户信息，用作远程调用接口
     * @param userId
     * @return
     */
    @ApiOperation("根据用户id查询用户认证姓名")
    @GetMapping("findUserById/{userId}")
    public UserInfo findUserById(@PathVariable Long userId){
        UserInfo userInfo = userInfoService.getById(userId);
        return userInfo;
    }
}
