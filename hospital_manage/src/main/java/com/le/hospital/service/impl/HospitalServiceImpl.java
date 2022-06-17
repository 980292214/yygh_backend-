package com.le.hospital.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.le.hospital.mapper.OrderInfoMapper;
import com.le.hospital.mapper.ScheduleMapper;
import com.le.hospital.model.OrderInfo;
import com.le.hospital.model.Patient;
import com.le.hospital.model.Schedule;
import com.le.hospital.service.HospitalService;
import com.le.hospital.util.ResultCodeEnum;
import com.le.hospital.util.YyghException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class HospitalServiceImpl implements HospitalService {

	@Autowired
	private ScheduleMapper hospitalMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;//自己加的，原来退号成功mongodb号源正常，mysql里的号源不会+1 5.8

    @Transactional(rollbackFor = Exception.class)
    @Override
    //预约下单--
    public Map<String, Object> submitOrder(Map<String, Object> paramMap) {
        log.info(JSONObject.toJSONString(paramMap));
        String hoscode = (String)paramMap.get("hoscode");
        String depcode = (String)paramMap.get("depcode");
        //医院排班id,此处请求方发送过来的id是从mongodb里的Schedule表查出来的HosScheduleID，填坑5.6
        String hosScheduleId = (String)paramMap.get("hosScheduleId");
        String reserveDate = (String)paramMap.get("reserveDate");
        String reserveTime = (String)paramMap.get("reserveTime");
        String amount = (String)paramMap.get("amount");

        //把下面要用到的HosScheduleId直接改为1L就好了，这里用的是模拟数据。5.6已改进
        //Schedule schedule = this.getSchedule("1L");//原来是1 有空指针异常,已解决
        //自动同步两张表的数据!？注意id要对应hosScheduleID,不能是自增。若想多个医院同时用一个（这个）系统,不同医院的排班的hosScheduleID要不同 5.6
        Schedule schedule = this.getSchedule(hosScheduleId);
        System.out.println("查到的schedule:"+schedule);//
        if(null == schedule) {
            System.out.println("查到的schedule为空");//
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }

        if(!schedule.getHoscode().equals(hoscode)
                || !schedule.getDepcode().equals(depcode)
                || !schedule.getAmount().toString().equals(amount)) {
            System.out.println("医院编号或科室编号或金额对应不上！");//
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }

        //就诊人信息
        Patient patient = JSONObject.parseObject(JSONObject.toJSONString(paramMap), Patient.class);//??
        log.info("就诊人信息:"+JSONObject.toJSONString(patient));
        //处理就诊人业务
        //Long patientId = this.savePatient(patient);//原来：固定返回1

        Map<String, Object> resultMap = new HashMap<>();
        int availableNumber = schedule.getAvailableNumber().intValue() - 1;//在mysql更新可预约数,但mongodb没有更新！更新了
        if(availableNumber >= 0) {//原来是>0，评论说为1时有错误
            schedule.setAvailableNumber(availableNumber);
            hospitalMapper.updateById(schedule);

            //记录预约记录
            OrderInfo orderInfo = new OrderInfo();
            //orderInfo.setPatientId(patientId);//原来固定是1
            System.out.println("paramMap.get(patient_id)"+paramMap.get("patient_id"));
            String patient_id = (String) paramMap.get("patient_id");//Object转为String再转为Long
            orderInfo.setPatientId(Long.parseLong(patient_id));//自己加的5.6
            //orderInfo.setScheduleId(1L);//原来：固定是1
            orderInfo.setScheduleId(Long.parseLong(hosScheduleId));//自己加的5.6
            int number = schedule.getReservedNumber().intValue() - schedule.getAvailableNumber().intValue();
            orderInfo.setNumber(number);//预约号序
            orderInfo.setAmount(new BigDecimal(amount));
            String fetchTime = "0".equals(reserveTime) ? " 09:30前" : " 14:00前";//原来是reserveDate
            orderInfo.setFetchTime(reserveTime + fetchTime);
            orderInfo.setFetchAddress("一楼8号窗口");
            //默认 未支付
            orderInfo.setOrderStatus(0);
            orderInfoMapper.insert(orderInfo);//插入 mysql

            resultMap.put("resultCode","0000");//前台没有体现这些数据？
            resultMap.put("resultMsg","预约成功");
            //预约记录唯一标识（医院预约记录主键）
            resultMap.put("hosRecordId", orderInfo.getId());
            //预约号序
            resultMap.put("number", number);
            //取号时间
            resultMap.put("fetchTime", reserveDate + " "+fetchTime);//原来是固定09:00
            //取号地址
            resultMap.put("fetchAddress", "一层8窗口");;
            //排班可预约数
            resultMap.put("reservedNumber", schedule.getReservedNumber());
            //排班剩余预约数
            resultMap.put("availableNumber", schedule.getAvailableNumber());
        } else {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        return resultMap;
    }
    //支付成功，更新支付状态，order模块微信支付成功后调用--
    @Override
    public void updatePayStatus(Map<String, Object> paramMap) {
        String hoscode = (String)paramMap.get("hoscode");
        String hosRecordId = (String)paramMap.get("hosRecordId");

        OrderInfo orderInfo = orderInfoMapper.selectById(hosRecordId);
        if(null == orderInfo) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        //已支付
        orderInfo.setOrderStatus(1);
        orderInfo.setPayTime(new Date());
        orderInfoMapper.updateById(orderInfo);
    }

    //更新取消预约状态--
    @Override
    public void updateCancelStatus(Map<String, Object> paramMap) {
        String hoscode = (String)paramMap.get("hoscode");
        String hosRecordId = (String)paramMap.get("hosRecordId");//预约记录唯一标识（医院预约记录主键）

        OrderInfo orderInfo = orderInfoMapper.selectById(hosRecordId);
        if(null == orderInfo) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        //已取消
        orderInfo.setOrderStatus(-1);
        orderInfo.setQuitTime(new Date());// 手动设置
        int i = orderInfoMapper.updateById(orderInfo);
        System.out.println("取消预约成功,更新了"+i+"条数据");//
        //更新排班，还原排班可预约数 原来有bug，退号成功mongodb号源正常，mysql里的号源不会+1，已修复5.8
        //System.out.println("订单里的ScheduleId是："+orderInfo.getScheduleId());测试通过5.8
        Schedule schedule = scheduleMapper.selectById(orderInfo.getScheduleId());
        schedule.setAvailableNumber(schedule.getAvailableNumber() + 1);
        int i1 = scheduleMapper.updateById(schedule);
        System.out.println("恢复号源成功数量："+i1);
    }

    //根据 id 查找 Schedule
    private Schedule getSchedule(String frontSchId) {
        return hospitalMapper.selectById(frontSchId);
    }

    /**
     * 医院处理就诊人信息--
     * @param patient
     */
    private Long savePatient(Patient patient) {
        // 业务：略
        return 1L;
    }


}
