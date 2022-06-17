package com.le.yygh.order.api;

import com.le.yygh.common.result.Result;
import com.le.yygh.order.service.PaymentService;
import com.le.yygh.order.service.WeixinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/order/weixin")
public class WeixinController {

    @Autowired
    private WeixinService weixinService;

    @Autowired
    private PaymentService paymentService;

    //生成微信支付二维码
    @GetMapping("createNative/{orderId}")
    public Result createNative(@PathVariable Long orderId) {
        Map map = weixinService.createNative(orderId);
        return Result.ok(map);
    }

    //查询支付状态，若支付成功，还会调用医院接口（另一个微服务），更新订单支付信息
    @GetMapping("queryPayStatus/{orderId}")
    public Result queryPayStatus(@PathVariable Long orderId) {
        //调用微信接口实现支付状态查询
        Map<String,String> resultMap = weixinService.queryPayStatus(orderId);
        //判断
        if(resultMap == null) {
            return Result.fail().message("支付出错");
        }
        if("SUCCESS".equals(resultMap.get("trade_state"))) { //支付成功
            //更新订单状态,还会调用医院接口
            String out_trade_no = resultMap.get("out_trade_no");//订单编码
            paymentService.paySuccess(out_trade_no,resultMap);
            return Result.ok().message("支付成功");
        }
        return Result.ok().message("支付中");
    }
}
