package com.le.yygh.order.api;

import com.le.yygh.common.result.Result;
import com.le.yygh.common.utils.AuthContextHolder;
import com.le.yygh.enums.OrderStatusEnum;
import com.le.yygh.model.order.OrderInfo;
import com.le.yygh.model.user.UserInfo;
import com.le.yygh.order.service.OrderService;
import com.le.yygh.vo.order.OrderCountQueryVo;
import com.le.yygh.vo.order.OrderQueryVo;
import com.le.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/order/orderInfo")
public class OrderApiController {

    @Autowired
    private OrderService orderService;

    //生成挂号订单
    @PostMapping("auth/submitOrder/{scheduleId}/{patientId}")
    public Result savaOrders(@PathVariable String scheduleId,
                             @PathVariable Long patientId) {
        Long orderId = orderService.saveOrder(scheduleId,patientId);
        return Result.ok(orderId);
    }

    //根据订单id查询订单详情,后台界面使用，前台用户也要查看订单详情
    @GetMapping(value = {"auth/getOrders/{orderId}"})//auth会被网关拦截
    public Result getOrders(@PathVariable String orderId) {
        OrderInfo orderInfo = orderService.getOrder(orderId);
        return Result.ok(orderInfo);
    }

    //订单列表（条件查询带分页）
    @GetMapping(value = {"auth/{page}/{limit}"})
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit,
                       OrderQueryVo orderQueryVo, HttpServletRequest request) {
        //设置当前用户id
        orderQueryVo.setUserId(AuthContextHolder.getUserId(request));
        Page<OrderInfo> pageParam = new Page<>(page,limit);
        IPage<OrderInfo> pageModel =
                orderService.selectPage(pageParam,orderQueryVo);
        return Result.ok(pageModel);
    }

    @GetMapping(value = {"{page}/{limit}"})//前台使用
    public Result list02(@PathVariable Long page,
                       @PathVariable Long limit
                       ) {
        //设置当前用户id
        Page<OrderInfo> pageParam = new Page<>(page,limit);
        IPage<OrderInfo> pageModel =
                orderService.selectPage(pageParam,null);
        return Result.ok(pageModel);
    }

    @ApiOperation(value = "获取订单状态")//谁调用？
    @GetMapping(value = {"auth/getStatusList"})
    public Result getStatusList() {
        return Result.ok(OrderStatusEnum.getStatusList());
    }

    //取消预约
    @GetMapping(value = {"auth/cancelOrder/{orderId}"})
    public Result cancelOrder(@PathVariable Long orderId) {
        Boolean isOrder = orderService.cancelOrder(orderId);
        return Result.ok(isOrder);
    }

    @ApiOperation(value = "获取订单统计数据")
    @PostMapping("inner/getCountMap")
    public Map<String, Object> getCountMap(@RequestBody OrderCountQueryVo orderCountQueryVo) {
        return orderService.getCountMap(orderCountQueryVo);
    }
}



