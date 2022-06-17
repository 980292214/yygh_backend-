package com.le.yygh.order.service;

import com.le.yygh.model.order.OrderInfo;
import com.le.yygh.model.user.UserInfo;
import com.le.yygh.vo.order.OrderCountQueryVo;
import com.le.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderService extends IService<OrderInfo> {

    //生成挂号订单
    Long saveOrder(String scheduleId, Long patientId);

    //根据订单id查询订单详情
    OrderInfo getOrder(String orderId);

    //订单列表（条件查询带分页）
    IPage<OrderInfo> selectPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo);

    //取消预约
    Boolean cancelOrder(Long orderId);

    //就诊通知
    void patientTips();

    //预约统计
    Map<String,Object> getCountMap(OrderCountQueryVo orderCountQueryVo );

    /**
     * 新加的，后台前端订单模块5.3
     * 根据 订单会员人、就诊人、订单状态等信息，查询订单列表，并进行分页
     * @param pageParam
     * @param orderQueryVo
     * @return
     */
    IPage<OrderInfo> selectAdminPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo);
}
