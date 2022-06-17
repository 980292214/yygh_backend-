package com.le.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.le.common.rabbit.constant.MqConst;
import com.le.common.rabbit.service.RabbitService;
import com.le.yygh.common.helper.HttpRequestHelper;
import com.le.yygh.enums.OrderStatusEnum;
import com.le.yygh.enums.PaymentStatusEnum;
import com.le.yygh.enums.PaymentTypeEnum;
import com.le.yygh.hosp.client.HospitalFeignClient;
import com.le.yygh.model.order.OrderInfo;
import com.le.yygh.model.order.PaymentInfo;
import com.le.yygh.order.mapper.OrderMapper;
import com.le.yygh.order.mapper.PaymentMapper;
import com.le.yygh.order.service.OrderService;
import com.le.yygh.order.service.PaymentService;
import com.le.yygh.vo.msm.MsmVo;
import com.le.yygh.vo.order.OrderMqVo;
import com.le.yygh.vo.order.SignInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl extends
        ServiceImpl<PaymentMapper, PaymentInfo> implements PaymentService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    private RabbitService rabbitService;//自己加5.2，发短信用的

    //向支付记录表添加信息
    @Override
    public void savePaymentInfo(OrderInfo order, Integer paymentType) {
        //根据订单id和支付类型，查询支付记录表是否存在相同订单
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",order.getId());
        wrapper.eq("payment_type",paymentType);
        Integer count = baseMapper.selectCount(wrapper);
        if(count > 0) {
            return;
        }
        //添加记录
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(order.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(order.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatusEnum.UNPAID.getStatus());
        String subject = new DateTime(order.getReserveDate()).toString("yyyy-MM-dd")+"|"+order.getHosname()+"|"+order.getDepname()+"|"+order.getTitle();
        paymentInfo.setSubject(subject);//交易内容
        paymentInfo.setTotalAmount(order.getAmount());
        baseMapper.insert(paymentInfo);
    }

    //更新订单状态，支付成功,还会调用医院接口（另一个微服务），更新订单支付信息
    @Override
    public void paySuccess(String out_trade_no, Map<String, String> resultMap) {
        //1 根据订单编号得到支付记录
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",out_trade_no);
        wrapper.eq("payment_type", PaymentTypeEnum.WEIXIN.getStatus());
        PaymentInfo paymentInfo = baseMapper.selectOne(wrapper);

        //2 更新支付记录信息
        paymentInfo.setPaymentStatus(PaymentStatusEnum.PAID.getStatus());//已支付
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setTradeNo(resultMap.get("transaction_id"));//交易编号，付款成功后才有
        paymentInfo.setCallbackContent(resultMap.toString());
        baseMapper.updateById(paymentInfo);

        //3 根据订单号得到订单信息
        //4 更新订单信息
        OrderInfo orderInfo = orderService.getById(paymentInfo.getOrderId());
        orderInfo.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        orderService.updateById(orderInfo);

        //5 调用医院接口，更新订单支付信息 TODO 5.14
        SignInfoVo signInfoVo = hospitalFeignClient.getSignInfoVo(orderInfo.getHoscode());
        Map<String,Object> reqMap = new HashMap<>();
        reqMap.put("hoscode",orderInfo.getHoscode());
        reqMap.put("hosRecordId",orderInfo.getHosRecordId());
        reqMap.put("timestamp", HttpRequestHelper.getTimestamp());
        String sign = HttpRequestHelper.getSign(reqMap, signInfoVo.getSignKey());
        reqMap.put("sign", sign);
        JSONObject result = HttpRequestHelper.sendRequest(reqMap, signInfoVo.getApiUrl() + "/order/updatePayStatus");
        System.out.println("调用医院接口，更新订单支付信息的返回JSONObject："+result);
        //发送短信给就诊人，自己加，已完成
        MsmVo msmVo = new MsmVo();
        System.out.println("就诊人的电话(发短信用)："+orderInfo.getPatientPhone());
        msmVo.setPhone(orderInfo.getPatientPhone());
        msmVo.setTemplateCode("1372409");//挂号成功的短信模板
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM, MqConst.ROUTING_MSM_ITEM, msmVo);
    }

    /**
     * 获取支付记录,微信退款时调用
     * @param orderId
     * @param paymentType
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(Long orderId, Integer paymentType) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderId);
        wrapper.eq("payment_type",paymentType);
        PaymentInfo paymentInfo = baseMapper.selectOne(wrapper);
        return paymentInfo;
    }

}
