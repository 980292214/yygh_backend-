package com.le.yygh.hosp.client;

import com.le.yygh.vo.hosp.ScheduleOrderVo;
import com.le.yygh.vo.order.SignInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-hosp")
@Repository
public interface HospitalFeignClient {

    /**
     * 根据排班id获取预约下单数据
     */
    @GetMapping("/api/hosp/hospital/inner/getScheduleOrderVo/{scheduleId}")
    ScheduleOrderVo getScheduleOrderVo(@PathVariable("scheduleId") String scheduleId);

    /**
     * 根据医院编号获取获取医院签名信息
     */
    @GetMapping("/api/hosp/hospital/inner/getSignInfoVo/{hoscode}")
    public SignInfoVo getSignInfoVo(@PathVariable("hoscode") String hoscode);

    //更新医院状态功能 上传schedule时同步两边的数据库 可不要此功能 自己加，5.10 //不同系统不能直接调用，可以删了
    @GetMapping("/admin/hosp/hospitalSet/getStatusByhoscode/{hoscode}")
    public Integer getStatusByhoscode(@PathVariable("hoscode") String hoscode);
}
