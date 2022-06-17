package com.le.yygh.user.client;

import com.le.yygh.model.user.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Component
@FeignClient("service-user")
public interface UserInfoFeignClient {//新加的，后台前端模糊查询使用 5.3

    /**
     * 根据用户认证的姓名，模糊查询叫该用户的所用用户列表，用作远程调用
     * @param username
     * @return
     */
    @GetMapping("/admin/user/findUserListByUserName/{username}")
    public List<UserInfo> findUserListByUserName(@PathVariable("username") String username);


    /**
     * 根据用户id返回用户信息，用作远程调用接口
     * @param userId
     * @return
     */
    @GetMapping("/admin/user/findUserById/{userId}")
    public UserInfo findUserById(@PathVariable("userId") Long userId);
}
