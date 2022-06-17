package com.le.yygh.task.scheduled;

import com.le.common.rabbit.constant.MqConst;
import com.le.common.rabbit.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling//开启定时任务操作
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    //每天8点执行方法；就医提醒，若就诊人明天有预约记录，就（今天）发送短信提醒他
    //cron表达式，设置执行间隔  http://cron.ciding.cc/
    @Scheduled(cron = "0 0 8 * * ?")// EXCHANGE_DIRECT_TASK 已被监听
    //@Scheduled(cron = "0/30 * * * * ?")//每隔30秒 测试用
    public void taskPatient() {
        //在order模块监听
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_8,"");
    }
}
