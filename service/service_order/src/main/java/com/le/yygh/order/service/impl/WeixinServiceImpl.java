package com.le.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.le.yygh.enums.PaymentTypeEnum;
import com.le.yygh.enums.RefundStatusEnum;
import com.le.yygh.model.order.OrderInfo;
import com.le.yygh.model.order.PaymentInfo;
import com.le.yygh.model.order.RefundInfo;
import com.le.yygh.order.service.OrderService;
import com.le.yygh.order.service.PaymentService;
import com.le.yygh.order.service.RefundInfoService;
import com.le.yygh.order.service.WeixinService;
import com.le.yygh.order.utils.ConstantPropertiesUtils;
import com.le.yygh.order.utils.HttpClient;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WeixinServiceImpl implements WeixinService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate redisTemplate;
    
    @Autowired
    private RefundInfoService refundInfoService;
    

    //生成微信支付二维码
    @Override
    public Map createNative(Long orderId) {
        try {
            //从redis获取数据//?
            Map payMap = (Map)redisTemplate.opsForValue().get(orderId.toString());
            if(payMap != null) {
                return payMap;
            }
            //1 根据orderId获取订单信息
            OrderInfo order = orderService.getById(orderId);
            //2 向支付记录表添加信息
            paymentService.savePaymentInfo(order, PaymentTypeEnum.WEIXIN.getStatus());
            //3设置参数，
            //把参数转换xml格式，使用商户key进行加密
            Map paramMap = new HashMap();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            String body = order.getReserveDate() + "就诊"+ order.getDepname();
            paramMap.put("body", body);
            paramMap.put("out_trade_no", order.getOutTradeNo());
            //paramMap.put("total_fee", order.getAmount().multiply(new BigDecimal("100")).longValue()+"");
            paramMap.put("total_fee", "1"); //(金额)为了测试，统一写成这个值
            paramMap.put("spbill_create_ip", "127.0.0.1");
            paramMap.put("notify_url", "http://guli.shop/api/order/weixinPay/weixinNotify");
            paramMap.put("trade_type", "NATIVE");//支付类型为扫描
            //4 调用微信生成二维码接口,httpclient根据URL访问第三方接口并且传递参数
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            //设置map参数
            client.setXmlParam(WXPayUtil.generateSignedXml(paramMap,ConstantPropertiesUtils.PARTNERKEY));
            client.setHttps(true);
            client.post();
            //5 返回相关数据
            String xml = client.getContent();
            //转换map集合
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            System.out.println("微信支付返回的resultMap:"+resultMap);
            //6 封装返回结果集
            Map map = new HashMap<>();
            map.put("orderId", orderId);
            map.put("totalFee", order.getAmount());
            map.put("resultCode", resultMap.get("result_code"));
            map.put("codeUrl", resultMap.get("code_url")); //二维码地址

            if(resultMap.get("result_code") != null) {//通过redis设置二维码的有效时间
                redisTemplate.opsForValue().set(orderId.toString(),map,30, TimeUnit.MINUTES);//30分钟过期
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //调用微信接口实现支付状态查询
    @Override
    public Map<String, String> queryPayStatus(Long orderId) {
        try {
            //1 根据orderId获取订单信息
            OrderInfo orderInfo = orderService.getById(orderId);

            //2 封装提交参数
            Map paramMap = new HashMap();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("out_trade_no", orderInfo.getOutTradeNo());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());

            //3 设置请求内容
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            client.setXmlParam(WXPayUtil.generateSignedXml(paramMap,ConstantPropertiesUtils.PARTNERKEY));
            client.setHttps(true);
            client.post();

            //4 得到微信接口返回数据
            String xml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            System.out.println("支付状态resultMap:"+resultMap);
            //5 把接口数据返回
            return resultMap;
        }catch(Exception e) {
            return null;
        }
    }

    //微信退款
    @Override
    public Boolean refund(Long orderId) {
        try {
            //获取支付记录信息
            PaymentInfo paymentInfo = paymentService.getPaymentInfo(orderId, PaymentTypeEnum.WEIXIN.getStatus());
            //添加信息到退款记录表（首次是插入insert）
            RefundInfo refundInfo = refundInfoService.saveRefundInfo(paymentInfo);
            //判断当前订单数据是否已经退款
            if(refundInfo.getRefundStatus().intValue() == RefundStatusEnum.REFUND.getStatus().intValue()) {
                return true;
            }
            //调用微信接口实现退款
            //封装需要参数
            Map<String,String> paramMap = new HashMap<>();
            paramMap.put("appid",ConstantPropertiesUtils.APPID);       //公众账号ID
            paramMap.put("mch_id",ConstantPropertiesUtils.PARTNER);   //商户编号
            paramMap.put("nonce_str",WXPayUtil.generateNonceStr()); //随机字符串？
            paramMap.put("transaction_id",paymentInfo.getTradeNo()); //微信订单号
            paramMap.put("out_trade_no",paymentInfo.getOutTradeNo()); //商户订单编号
            paramMap.put("out_refund_no","tk"+paymentInfo.getOutTradeNo()); //商户退款单号
//       paramMap.put("total_fee",paymentInfoQuery.getTotalAmount().multiply(new BigDecimal("100")).longValue()+"");
//       paramMap.put("refund_fee",paymentInfoQuery.getTotalAmount().multiply(new BigDecimal("100")).longValue()+"");
            paramMap.put("total_fee","1");//可以自己设置金额？
            paramMap.put("refund_fee","1");
            String paramXml = WXPayUtil.generateSignedXml(paramMap,ConstantPropertiesUtils.PARTNERKEY);
            //设置调用接口内容
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/secapi/pay/refund");
            client.setXmlParam(paramXml);
            client.setHttps(true);
            //设置证书信息,不用填证书位置？
            client.setCert(true);
            client.setCertPassword(ConstantPropertiesUtils.PARTNER);
            client.post();

            //接收返回数据
            String xml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            if (null != resultMap && WXPayConstants.SUCCESS.equalsIgnoreCase(resultMap.get("result_code"))) {
                refundInfo.setCallbackTime(new Date());
                refundInfo.setTradeNo(resultMap.get("refund_id"));
                refundInfo.setRefundStatus(RefundStatusEnum.REFUND.getStatus());//已退款
                refundInfo.setCallbackContent(JSONObject.toJSONString(resultMap));
                refundInfoService.updateById(refundInfo);//更新退款表
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
