package com.le.yygh.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.le.yygh.common.exception.YyghException;
import com.le.yygh.common.helper.JwtHelper;
import com.le.yygh.common.result.ResultCodeEnum;
import com.le.yygh.enums.AuthStatusEnum;
import com.le.yygh.model.user.Patient;
import com.le.yygh.model.user.UserInfo;
import com.le.yygh.user.mapper.UserInfoMapper;
import com.le.yygh.user.service.PatientService;
import com.le.yygh.user.service.UserInfoService;
import com.le.yygh.vo.user.LoginVo;
import com.le.yygh.vo.user.UserAuthVo;
import com.le.yygh.vo.user.UserInfoQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends
        ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PatientService patientService;

    //用户手机号登录接口
    @Override
    public Map<String, Object> loginUser(LoginVo loginVo) {
        //从loginVo获取输入的手机号，和验证码
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();

        //判断手机号和验证码是否为空
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)) {
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }

        //判断手机验证码和输入的验证码是否一致@@@@
        String redisCode = redisTemplate.opsForValue().get(phone);
        if (!code.equals(redisCode)) {
            throw new YyghException(ResultCodeEnum.CODE_ERROR);
        }

        //绑定手机号码
        UserInfo userInfo = null;
        if (!StringUtils.isEmpty(loginVo.getOpenid())) {//getOpenid里有值就说明要绑定手机号
            userInfo = this.selectWxInfoOpenId(loginVo.getOpenid());//若数据库里 openid 已经存在，就返回对应的用户信息
            if (null != userInfo) {//若数据库里有用户信息，则绑定
                userInfo.setPhone(loginVo.getPhone());
                this.updateById(userInfo);
            } else {
                throw new YyghException(ResultCodeEnum.DATA_ERROR);
            }
        }

        //如果userinfo为空，进行正常手机登录
        if (userInfo == null) {
            //判断是否第一次登录：根据手机号查询数据库，如果不存在相同手机号就是第一次登录
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("phone", phone);
            userInfo = baseMapper.selectOne(wrapper);
            if (userInfo == null) { //第一次使用这个手机号登录
                //添加信息到数据库
                userInfo = new UserInfo();
                userInfo.setName("");
                userInfo.setPhone(phone);
                userInfo.setStatus(1);//0:被锁定；1：正常
                baseMapper.insert(userInfo);
            }
        }

        //校验是否被禁用--
        if (userInfo.getStatus() == 0) {
            throw new YyghException(ResultCodeEnum.LOGIN_DISABLED_ERROR);
        }

        //不是第一次，直接登录
        //返回登录信息
        //返回登录用户名
        //返回token信息
        Map<String, Object> map = new HashMap<>();
        String name = userInfo.getName();//实名
        if (StringUtils.isEmpty(name)) {
            name = userInfo.getNickName();
        }
        if (StringUtils.isEmpty(name)) {//若昵称也为空就返回手机号
            name = userInfo.getPhone();
        }
        map.put("name", name);

        //jwt生成token字符串
        String token = JwtHelper.createToken(userInfo.getId(), name);
        map.put("token", token);
        return map;//返回token
    }

    //若数据库里 openid 已经存在，就返回对应的用户信息
    @Override
    public UserInfo selectWxInfoOpenId(String openid) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid", openid);
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);
        return userInfo;
    }

    //用户认证
    @Override
    public void userAuth(Long userId, UserAuthVo userAuthVo) {
        //根据用户id查询用户信息
        UserInfo userInfo = baseMapper.selectById(userId);
        //设置认证信息
        //认证人姓名
        userInfo.setName(userAuthVo.getName());
        //其他认证信息
        userInfo.setCertificatesType(userAuthVo.getCertificatesType());
        userInfo.setCertificatesNo(userAuthVo.getCertificatesNo());
        userInfo.setCertificatesUrl(userAuthVo.getCertificatesUrl());
        userInfo.setAuthStatus(AuthStatusEnum.AUTH_RUN.getStatus());
        //进行信息更新
        baseMapper.updateById(userInfo);
    }

    //用户列表（条件查询带分页）
    @Override
    public IPage<UserInfo> selectPage(Page<UserInfo> pageParam, UserInfoQueryVo userInfoQueryVo) {
        //UserInfoQueryVo获取条件值
        String name = userInfoQueryVo.getKeyword(); //用户名称
        Integer status = userInfoQueryVo.getStatus();//用户状态
        Integer authStatus = userInfoQueryVo.getAuthStatus(); //认证状态
        String createTimeBegin = userInfoQueryVo.getCreateTimeBegin(); //用户创建开始时间，用于后台查询
        String createTimeEnd = userInfoQueryVo.getCreateTimeEnd(); //用户创建结束时间
        //对条件值进行非空判断
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(name)) {//Wapper里可以传个条件参数，就不用写if了?
            wrapper.like("name", name);
        }
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("status", status);
        }
        if (!StringUtils.isEmpty(authStatus)) {
            wrapper.eq("auth_status", authStatus);
        }
        if (!StringUtils.isEmpty(createTimeBegin)) {
            wrapper.ge("create_time", createTimeBegin);
        }
        if (!StringUtils.isEmpty(createTimeEnd)) {
            wrapper.le("create_time", createTimeEnd);
        }
        //调用mapper的方法
        IPage<UserInfo> pages = baseMapper.selectPage(pageParam, wrapper);
        //编号变成对应值封装
        pages.getRecords().stream().forEach(item -> {//pages.getRecords()返回的是 分页对象记录列表
            this.packageUserInfo(item);
        });
        return pages;//分页对象记录列表
    }

    //用户锁定
    @Override
    public void lock(Long userId, Integer status) {
        if (status.intValue() == 0 || status.intValue() == 1) {
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setStatus(status);
            baseMapper.updateById(userInfo);
        }
    }

    //用户详情
    @Override
    public Map<String, Object> show(Long userId) {
        Map<String, Object> map = new HashMap<>();
        //根据userid查询用户信息
        UserInfo userInfo = this.packageUserInfo(baseMapper.selectById(userId));
        map.put("userInfo", userInfo);
        //根据userid查询就诊人信息
        List<Patient> patientList = patientService.findAllUserId(userId);
        map.put("patientList", patientList);
        return map;
    }

    //认证审批  2通过  -1不通过
    @Override
    public void approval(Long userId, Integer authStatus) {
        if (authStatus.intValue() == 2 || authStatus.intValue() == -1) {
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setAuthStatus(authStatus);
            baseMapper.updateById(userInfo);
        }
    }


    //编号变成对应值封装(前端处理更好!?)
    private UserInfo packageUserInfo(UserInfo userInfo) {
        //处理认证状态编码
        userInfo.getParam().put("authStatusString", AuthStatusEnum.getStatusNameByStatus(userInfo.getAuthStatus()));
        //处理用户状态 0  1
        String statusString = userInfo.getStatus().intValue() == 0 ? "锁定" : "正常";
        //Integer integer = userInfo.getStatus();//对象转int
        userInfo.getParam().put("statusString", statusString);
        return userInfo;
    }

    /**
     * 新加的，后台前端模糊查询使用 5.3
     * 根据用户认证的姓名，模糊查询叫该用户的所用用户列表
     *
     * @param username
     * @return
     */
    @Override
    public List<UserInfo> findUserListByUserName(String username) {
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.like("name", username);
        List<UserInfo> userInfoList = baseMapper.selectList(wrapper);//没有分页？
        return userInfoList;
    }
}
