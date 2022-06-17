package com.le.yygh.hosp.service;

import com.le.yygh.model.hosp.Schedule;
import com.le.yygh.vo.hosp.ScheduleOrderVo;
import com.le.yygh.vo.hosp.ScheduleQueryVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ScheduleService {
    //上传排班接口
    void save(Map<String, Object> paramMap);

    //查询排班接口
    Page<Schedule> findPageSchedule(int page, int limit, ScheduleQueryVo scheduleQueryVo);

    //删除排班
    void remove(String hoscode, String hosScheduleId);

    //根据医院编号 和 科室编号 ，查询排班规则数据
    Map<String, Object> getRuleSchedule(long page, long limit, String hoscode, String depcode);

    //根据医院编号 、科室编号和工作日期，查询排班详细信息
    List<Schedule> getDetailSchedule(String hoscode, String depcode, String workDate);

    //获取可预约的排班数据
    Map<String,Object> getBookingScheduleRule(int page,int limit,String hoscode,String depcode);

    //根据排班id获取排班数据-
    Schedule getScheduleId(String scheduleId);

    //根据hosScheduleID获取排班数据,因为改了原来的order_info表存的字段scheduleId，原来存的是mongodb的_id(字符串),现在是hosScheduleID 5.6
    Schedule getScheduleIdCancel(String scheduleId);

    //根据排班id获取预约下单数据，order模块生成订单时调用
    ScheduleOrderVo getScheduleOrderVo(String scheduleId);

    //更新排班数据 用于mq
    //用户下单成功后会更新
    void update(Schedule schedule);
}
