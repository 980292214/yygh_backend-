package com.le.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.le.common.rabbit.constant.MqConst;
import com.le.common.rabbit.service.RabbitService;
import com.le.yygh.common.exception.YyghException;
import com.le.yygh.common.helper.HttpRequestHelper;
import com.le.yygh.common.result.ResultCodeEnum;
import com.le.yygh.enums.OrderStatusEnum;
import com.le.yygh.hosp.client.HospitalFeignClient;
import com.le.yygh.model.order.OrderInfo;
import com.le.yygh.model.user.Patient;
import com.le.yygh.model.user.UserInfo;
import com.le.yygh.order.mapper.OrderMapper;
import com.le.yygh.order.service.OrderService;
import com.le.yygh.order.service.WeixinService;
import com.le.yygh.user.client.PatientFeignClient;
import com.le.yygh.user.client.UserInfoFeignClient;
import com.le.yygh.vo.hosp.ScheduleOrderVo;
import com.le.yygh.vo.msm.MsmVo;
import com.le.yygh.vo.order.*;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends
        ServiceImpl<OrderMapper, OrderInfo> implements OrderService {

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private WeixinService weixinService;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;//新加的，后台前端模糊查询使用 5.3

    //生成挂号订单，远程调用2个微服务
    @Override
    public Long saveOrder(String scheduleId, Long patientId) {
        //获取就诊人信息
        Patient patient = patientFeignClient.getPatientOrder(patientId);

        //获取排班相关信息
        ScheduleOrderVo scheduleOrderVo = hospitalFeignClient.getScheduleOrderVo(scheduleId);
        //System.out.println("scheduleId="+scheduleId+";patientId="+patientId);//测试完了
        System.out.println("开始放号时间：" + scheduleOrderVo.getStartTime());
        System.out.println("结束挂号时间：" + scheduleOrderVo.getStopTime());
        //判断当前时间是否还可以预约
        if (new DateTime(scheduleOrderVo.getStartTime()).isAfterNow()//允许挂号的时间在现在之后
                || new DateTime(scheduleOrderVo.getEndTime()).isBeforeNow()) {
            throw new YyghException(ResultCodeEnum.TIME_NO);
        }


        //获取签名信息
        SignInfoVo signInfoVo = hospitalFeignClient.getSignInfoVo(scheduleOrderVo.getHoscode());

        //添加到订单表
        OrderInfo orderInfo = new OrderInfo();
        //scheduleOrderVo 数据复制到 orderInfo
        BeanUtils.copyProperties(scheduleOrderVo, orderInfo);
        //向orderInfo设置其他数据
        String outTradeNo = System.currentTimeMillis() + "" + new Random().nextInt(100);//订单交易号
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setScheduleId(scheduleOrderVo.getHosScheduleId());//排班编号（医院自己的排班主键）;评论是对的5.6
        //orderInfo.setScheduleId(scheduleId);//原来写法:scheduleId是mongodb里最前面的_id,是字符串
        orderInfo.setUserId(patient.getUserId());
        orderInfo.setPatientId(patientId);
        orderInfo.setPatientName(patient.getName());
        orderInfo.setPatientPhone(patient.getPhone());
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());//预约成功，待支付
        baseMapper.insert(orderInfo);

        //调用医院接口，实现预约挂号操作
        //设置调用医院接口需要参数，参数放到map集合
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("hoscode", orderInfo.getHoscode());
        paramMap.put("depcode", orderInfo.getDepcode());
        paramMap.put("hosScheduleId", orderInfo.getScheduleId());//医院排班id，有坑，已解决5.6
        //paramMap.put("hosScheduleId", scheduleOrderVo.getHosScheduleId());//已改正 和上面的一样了5.6
        paramMap.put("reserveDate", new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd"));//安排日期
        paramMap.put("reserveTime", orderInfo.getReserveTime());//安排时间
        paramMap.put("amount", orderInfo.getAmount());

        paramMap.put("patient_id", patientId);//自己加5.6
        paramMap.put("name", patient.getName());
        paramMap.put("certificatesType", patient.getCertificatesType());
        paramMap.put("certificatesNo", patient.getCertificatesNo());
        paramMap.put("sex", patient.getSex());
        paramMap.put("birthdate", patient.getBirthdate());
        paramMap.put("phone", patient.getPhone());
        paramMap.put("isMarry", patient.getIsMarry());
        paramMap.put("provinceCode", patient.getProvinceCode());
        paramMap.put("cityCode", patient.getCityCode());
        paramMap.put("districtCode", patient.getDistrictCode());
        paramMap.put("address", patient.getAddress());
        //联系人
        paramMap.put("contactsName", patient.getContactsName());
        paramMap.put("contactsCertificatesType", patient.getContactsCertificatesType());
        paramMap.put("contactsCertificatesNo", patient.getContactsCertificatesNo());
        paramMap.put("contactsPhone", patient.getContactsPhone());
        paramMap.put("timestamp", HttpRequestHelper.getTimestamp());

        String sign = HttpRequestHelper.getSign(paramMap, signInfoVo.getSignKey());//用md5加密一下
        paramMap.put("sign", sign);

        //请求医院系统接口,访问h_manage模块//报了空指针异常，原因是查的yygh_hosp的表的apiurl有问题
        JSONObject result = HttpRequestHelper.sendRequest(paramMap, signInfoVo.getApiUrl() + "/order/submitOrder");
        System.out.println("空指针debug：返回的result为" + result);
        if (result.getInteger("code") == 200) {//报201异常了,原因是医院编号或科室编号或金额对应不上。
            JSONObject jsonObject = result.getJSONObject("data");
            //预约记录唯一标识（医院预约记录主键）
            String hosRecordId = jsonObject.getString("hosRecordId");
            //预约序号
            Integer number = jsonObject.getInteger("number");
            //取号时间
            String fetchTime = jsonObject.getString("fetchTime");
            //取号地址
            String fetchAddress = jsonObject.getString("fetchAddress");
            //更新订单
            orderInfo.setHosRecordId(hosRecordId);
            orderInfo.setNumber(number);
            orderInfo.setFetchTime(fetchTime);
            orderInfo.setFetchAddress(fetchAddress);
            baseMapper.updateById(orderInfo);
            //排班可（总）预约数
            Integer reservedNumber = jsonObject.getInteger("reservedNumber");
            //排班剩余预约数
            Integer availableNumber = jsonObject.getInteger("availableNumber");
            //发送mq消息，号源更新和短信通知
            //发送mq信息更新号源
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(scheduleId);//传进来的字符串，而不是hosScheduleID
            orderMqVo.setReservedNumber(reservedNumber);
            orderMqVo.setAvailableNumber(availableNumber);
            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            msmVo.setTemplateCode("1372409");//挂号成功的短信模板
            String reserveDate = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd") + (orderInfo.getReserveTime() == 0 ? "上午" : "下午");
            Map<String, Object> param = new HashMap<String, Object>() {{//腾讯云短信服务没有升级为企业版,无法动态设置文字
                put("title", orderInfo.getHosname() + "|" + orderInfo.getDepname() + "|" + orderInfo.getTitle());
                put("amount", orderInfo.getAmount());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
                put("quitTime", new DateTime(orderInfo.getQuitTime()).toString("yyyy-MM-dd HH:mm"));
            }};
            msmVo.setParam(param);
            //orderMqVo.setMsmVo(msmVo);//还没付款，先不发短信
            //参数：1.交换机 2.路由键 3.相应消息
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);//发短信了吗？会发的！
            System.out.println("应该发送的短信信息（这里是付款前）：" + param.toString());//测试
        } else {
            throw new YyghException(result.getString("message"), ResultCodeEnum.FAIL.getCode());//数据异常
        }
        return orderInfo.getId();//返回唯一的订单编号
    }

    //根据订单id查询订单详情
    @Override
    public OrderInfo getOrder(String orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        return this.packOrderInfo(orderInfo);
    }

    //订单列表（条件查询带分页）
    @Override
    public IPage<OrderInfo> selectPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo) {
        //orderQueryVo获取条件值
        String name = orderQueryVo.getKeyword(); //医院名称
        Long patientId = orderQueryVo.getPatientId(); //就诊人id，前端是下拉列表
        String orderStatus = orderQueryVo.getOrderStatus(); //订单状态
        String reserveDate = orderQueryVo.getReserveDate();//安排时间
        String createTimeBegin = orderQueryVo.getCreateTimeBegin();//似乎没用上
        String createTimeEnd = orderQueryVo.getCreateTimeEnd();

        //对条件值进行非空判断
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(name)) {
            wrapper.like("hosname", name);
        }
        if (!StringUtils.isEmpty(patientId)) {
            wrapper.eq("patient_id", patientId);//like 也行???
        }
        if (!StringUtils.isEmpty(orderStatus)) {
            wrapper.eq("order_status", orderStatus);
        }
        if (!StringUtils.isEmpty(reserveDate)) {
            wrapper.ge("reserve_date", reserveDate);
        }
        if (!StringUtils.isEmpty(createTimeBegin)) {
            wrapper.ge("create_time", createTimeBegin);
        }
        if (!StringUtils.isEmpty(createTimeEnd)) {
            wrapper.le("create_time", createTimeEnd);
        }
        wrapper.orderByDesc("update_time");
        //调用mapper的方法
        IPage<OrderInfo> pages = baseMapper.selectPage(pageParam, wrapper);
        //编号变成对应值封装
        pages.getRecords().stream().forEach(item -> {
            this.packOrderInfo(item);
        });
        return pages;
    }

    //取消预约
    @Override
    public Boolean cancelOrder(Long orderId) {
        boolean intend = false;//判断是否发短信，只有退款成功后才发
        //获取订单信息
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        //判断是否取消
        DateTime quitTime = new DateTime(orderInfo.getQuitTime());//是根据数据库的排班日期计算出来的，如就诊前一天的16：30
        System.out.println("执行取消预约操作了,但不一定成功：订单里的quitTime为：" + orderInfo.getQuitTime());//
        if (quitTime.isBeforeNow()) {//若可以退号时间比现在早，说明已过退号时间
            throw new YyghException(ResultCodeEnum.CANCEL_ORDER_NO);
        }
        //调用医院接口实现预约取消
        SignInfoVo signInfoVo = hospitalFeignClient.getSignInfoVo(orderInfo.getHoscode());
        if (null == signInfoVo) {
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("hoscode", orderInfo.getHoscode());
        reqMap.put("hosRecordId", orderInfo.getHosRecordId());
        reqMap.put("timestamp", HttpRequestHelper.getTimestamp());
        String sign = HttpRequestHelper.getSign(reqMap, signInfoVo.getSignKey());//未
        reqMap.put("sign", sign);

        JSONObject result = HttpRequestHelper.sendRequest(reqMap,
                signInfoVo.getApiUrl() + "/order/updateCancelStatus");
        //根据医院接口返回数据
        //System.out.println("调用医院接口实现预约取消操作，医院的返回为：" + result);//医院那边没问题
        if (result.getInteger("code") != 200) {//若医院系统返回无异常
            throw new YyghException(result.getString("message"), ResultCodeEnum.FAIL.getCode());
        } else {
            //判断当前订单是否已支付
            if (orderInfo.getOrderStatus().intValue() == OrderStatusEnum.PAID.getStatus().intValue()) {//若微信已支付
                Boolean isRefund = weixinService.refund(orderId);//微信退款
                if (!isRefund) {//退款失败
                    throw new YyghException(ResultCodeEnum.CANCEL_ORDER_FAIL);
                } else {
                    intend = true;//发送信息提醒客户
                }
            }
            //更新订单状态
            orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
            baseMapper.updateById(orderInfo);

            //发送mq更新预约数量
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(orderInfo.getScheduleId());//原来是_id(字符串)。改为是hosScheduleID了，报错5.6
            //短信提示
            if (intend) {//退款成功才发短信
                MsmVo msmVo = new MsmVo();
                msmVo.setPhone(orderInfo.getPatientPhone());
                msmVo.setTemplateCode("1390413");//成功取消并退款的短信模板
                String reserveDate = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd") + (orderInfo.getReserveTime() == 0 ? "上午" : "下午");
                Map<String, Object> param = new HashMap<String, Object>() {{
                    put("title", orderInfo.getHosname() + "|" + orderInfo.getDepname() + "|" + orderInfo.getTitle());
                    put("reserveDate", reserveDate);
                    put("name", orderInfo.getPatientName());
                }};
                msmVo.setParam(param);
                orderMqVo.setMsmVo(msmVo);//短信模板不一样
            }
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);
        }
        return true;
    }

    //就诊通知；短信模板不同
    @Override
    public void patientTips() {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        //wrapper.eq("reserve_date", new DateTime().toString("yyyy-MM-dd"));原来是当天提醒
        Date now = new Date();
        DateTime endTime = new DateTime(now.getTime() + 1000 * 60 * 60 * 24 * 1);//加一天,提前一天提醒//
        wrapper.eq("reserve_date", endTime.toString("yyyy-MM-dd"));
        //wrapper.ne("order_status", OrderStatusEnum.CANCLE.getStatus());//状态不为退款,查的是order模块的order表??
        wrapper.eq("order_status", OrderStatusEnum.PAID.getStatus());//状态为已付款
        List<OrderInfo> orderInfoList = baseMapper.selectList(wrapper);
        for (OrderInfo orderInfo : orderInfoList) {
            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            msmVo.setTemplateCode("1390477");//1390477 是提前一天的短信模板
            String reserveDate = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd") + (orderInfo.getReserveTime() == 0 ? "上午" : "下午");
            Map<String, Object> param = new HashMap<String, Object>() {{
                put("title", orderInfo.getHosname() + "|" + orderInfo.getDepname() + "|" + orderInfo.getTitle());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
            }};
            msmVo.setParam(param);
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM, MqConst.ROUTING_MSM_ITEM, msmVo);
            System.out.println("每日就医提醒理论上应该发的信息是：" + param);
        }
    }

    //预约统计
    @Override
    public Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo) {
        //调用mapper方法得到数据，自己要写sql
        List<OrderCountVo> orderCountVoList = baseMapper.selectOrderCount(orderCountQueryVo);//List方便转为Json数组，Echars要Json数组

        //获取x需要数据 ，日期数据  list集合
        List<String> dateList = orderCountVoList.stream().map(OrderCountVo::getReserveDate).collect(Collectors.toList());

        //获取y需要数据，具体数量  list集合
        List<Integer> countList = orderCountVoList.stream().map(OrderCountVo::getCount).collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        map.put("dateList", dateList);
        map.put("countList", countList);
        return map;
    }

    //把状态码转成文字
    private OrderInfo packOrderInfo(OrderInfo orderInfo) {
        orderInfo.getParam().put("orderStatusString", OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        return orderInfo;
    }

    /**
     * 新加的，后台前端订单模块5.3
     * 根据 订单会员人、就诊人、订单状态等信息，查询订单列表，并进行分页
     *
     * @param pageParam
     * @param orderQueryVo
     * @return
     */
    @Override
    public IPage<OrderInfo> selectAdminPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo) {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();

        //根据就诊人姓名模糊查询
        String patientName = orderQueryVo.getPatientName();
        if (!StringUtils.isEmpty(patientName)) {
            wrapper.like("patient_name", patientName);
        }

        //根据用户姓名模糊查询
        String userName = orderQueryVo.getUserName();
        if (!StringUtils.isEmpty(userName)) {
            //远程调用，获取用户列表
            List<UserInfo> userInfoList = userInfoFeignClient.findUserListByUserName(userName);

            List<Long> userNameList = new ArrayList<>();

            if (userInfoList != null && userInfoList.size() != 0) {
                //如果查询有响应用户，则对这些id进行查询
                for (UserInfo userInfo : userInfoList) {
                    userNameList.add(userInfo.getId());
                }
            } else {
                //该用户名没有用户，那么就让wraper查询用户id为-1的用户，空是查所有！
                userNameList.add(-1L);
            }
            wrapper.in("user_id", userNameList);
        }

        //根据订单状态查询
        String orderStatus = orderQueryVo.getOrderStatus();
        if (!StringUtils.isEmpty(orderStatus)) {
            wrapper.eq("order_status", orderStatus);//原来是like，有bug
        }
        wrapper.orderByDesc("update_time");//排序
        System.out.println("接收到的分页参数current为"+pageParam.getCurrent()+"-每页显示(Size)为"+pageParam.getSize());//测试可以正常接收，但前台没分页信息
        IPage<OrderInfo> pages = baseMapper.selectPage(pageParam, wrapper);
        System.out.println("分页查询后的pagesRecords为:"+pages.getRecords());//全都查出来了？？没加config，5.3已解决
        //编号变成对应值的封装
        pages.getRecords().stream().forEach(item -> {
            Long userId = item.getUserId();//订单会员名称
            item.getParam().put("userName", userInfoFeignClient.findUserById(userId).getName());//封装订单会员名称//空指针异常5.26,用户表数据不能乱删
            this.packOrderInfo(item); //订单状态
        });
        return pages;
    }

}
