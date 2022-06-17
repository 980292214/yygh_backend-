package com.le.yygh.user.service;

import com.le.yygh.model.hosp.HospitalSet;
import com.le.yygh.model.user.UserInfo;
import com.le.yygh.vo.user.LoginVo;
import com.le.yygh.vo.user.UserAuthVo;
import com.le.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    //用户手机号登录接口
    Map<String, Object> loginUser(LoginVo loginVo);

    //根据openid判断是否存在微信的扫描人信息
    UserInfo selectWxInfoOpenId(String openid);

    //用户认证
    void userAuth(Long userId, UserAuthVo userAuthVo);

    //用户列表（条件查询带分页）
    IPage<UserInfo> selectPage(Page<UserInfo> pageParam, UserInfoQueryVo userInfoQueryVo);

    //用户锁定
    void lock(Long userId, Integer status);

    //用户详情
    Map<String, Object> show(Long userId);

    //认证审批
    void approval(Long userId, Integer authStatus);

    /**新加的，后台前端模糊查询使用 5.3
     * 根据用户认证的姓名，模糊查询叫该用户的所用用户列表
     * @param username
     * @return
     */
    List<UserInfo> findUserListByUserName(String username);
}
