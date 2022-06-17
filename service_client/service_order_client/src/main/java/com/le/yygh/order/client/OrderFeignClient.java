package com.le.yygh.order.client;

import com.le.yygh.vo.order.OrderCountQueryVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(value = "service-order")
@Repository
public interface OrderFeignClient {
    /**
     * 获取统计数据 Echarts
     */
    @PostMapping("/api/order/orderInfo/inner/getCountMap")
    public Map<String, Object> getCountMap(@RequestBody OrderCountQueryVo orderCountQueryVo);

}
