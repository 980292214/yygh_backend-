package com.le.yygh.hosp.repository;

import com.le.yygh.model.hosp.Schedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ScheduleRepository extends MongoRepository<Schedule,String> {
    //根据医院编号 和 排班编号查询
    Schedule getScheduleByHoscodeAndHosScheduleId(String hoscode, String hosScheduleId);

    //根据医院编号 、科室编号和工作日期，查询排班详细信息
    List<Schedule> findScheduleByHoscodeAndDepcodeAndWorkDate(String hoscode, String depcode, Date toDate);

    //根据hosScheduleID获取排班数据,因为改了原来的order_info表存的字段scheduleId，原来存的是mongodb的_id(字符串),现在是hosScheduleID 5.6
    Schedule getScheduleByHosScheduleId(String scheduleId);//findScheduleByHosScheduleId
}
