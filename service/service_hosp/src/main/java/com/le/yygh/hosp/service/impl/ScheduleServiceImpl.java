package com.le.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.le.yygh.common.exception.YyghException;
import com.le.yygh.common.result.ResultCodeEnum;
import com.le.yygh.hosp.mapper.ScheduleMapper;
import com.le.yygh.hosp.repository.ScheduleRepository;
import com.le.yygh.hosp.service.DepartmentService;
import com.le.yygh.hosp.service.HospitalService;
import com.le.yygh.hosp.service.HospitalSetService;
import com.le.yygh.hosp.service.ScheduleService;
import com.le.yygh.model.hosp.BookingRule;
import com.le.yygh.model.hosp.Department;
import com.le.yygh.model.hosp.Hospital;
import com.le.yygh.model.hosp.Schedule;
import com.le.yygh.vo.hosp.BookingScheduleRuleVo;
import com.le.yygh.vo.hosp.ScheduleOrderVo;
import com.le.yygh.vo.hosp.ScheduleQueryVo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl extends
        ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private HospitalSetService hospitalSetService;//更新医院设置锁定功能 5.9

    //上传排班接口--医院接口模拟系统处使用（mongodb）
    @Override
    public void save(Map<String, Object> paramMap) {
        //paramMap 转换department对象
        String paramMapString = JSONObject.toJSONString(paramMap);
        Schedule schedule = JSONObject.parseObject(paramMapString, Schedule.class);

        //先判断医院设置状态，若已锁定，则不能上传 5.9
        String hoscode = schedule.getHoscode();
        //Hospital hospital = hospitalService.getByHoscode(hoscode);//这里是mongodb,要去mysql里查 5.9
        Integer hospitalStatus = hospitalSetService.getStatusByhoscode(hoscode);//去mysql里查
        //System.out.println("获取到的医院设置状态为："+hospitalStatus);
        if (hospitalStatus==0){
            System.out.println("该医院已被锁定，无法上传排班信息！");
            throw new YyghException(ResultCodeEnum.HOSPITAL_LOCK);
        }

        //根据医院编号 和 排班编号查询
        Schedule scheduleExist = scheduleRepository.
                getScheduleByHoscodeAndHosScheduleId(schedule.getHoscode(), schedule.getHosScheduleId());

        //判断
        if (scheduleExist != null) {
            schedule.setId(scheduleExist.getId());
            schedule.setCreateTime(scheduleExist.getCreateTime());
            schedule.setUpdateTime(new Date());//+++
            schedule.setIsDeleted(0);
            schedule.setStatus(1);
            scheduleRepository.save(schedule);
        } else {
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            schedule.setStatus(1);
            scheduleRepository.save(schedule);
        }
    }

    //查询排班接口，未--
    @Override
    public Page<Schedule> findPageSchedule(int page, int limit, ScheduleQueryVo scheduleQueryVo) {
        // 创建Pageable对象，设置当前页和每页记录数
        //0是第一页
        Pageable pageable = PageRequest.of(page - 1, limit);// TODO
        // 创建Example对象
        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(scheduleQueryVo, schedule);
        schedule.setIsDeleted(0);
        schedule.setStatus(1);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)//模糊查询？
                .withIgnoreCase(true);
        Example<Schedule> example = Example.of(schedule, matcher);

        Page<Schedule> all = scheduleRepository.findAll(example, pageable);
        return all;
    }

    //删除排班
    @Override
    public void remove(String hoscode, String hosScheduleId) {
        //根据医院编号和排班编号查询信息
        Schedule schedule = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(hoscode, hosScheduleId);
        if (schedule != null) {
            scheduleRepository.deleteById(schedule.getId());
        }
    }

    //根据医院编号 和 科室编号 ，查询排班规则数据 已-----
    @Override
    public Map<String, Object> getRuleSchedule(long page, long limit, String hoscode, String depcode) {
        //1 根据医院编号 和 科室编号 查询
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);

        //2 根据工作日workDate进行分组
        //MongoTemplate 通过 Aggregation 来做聚合操作，如分组、求和
        Aggregation agg = Aggregation.newAggregation(// agg 是聚合条件
                Aggregation.match(criteria),//匹配条件
                Aggregation.group("workDate")//分组字段
                        // .first() 获取group by 后的某个字段的首个值
                        .first("workDate").as("workDate")//别名是查询结果集转化成vo对象用的,别名要对应成员变量的?
                        //3 统计号源数量；就诊的医生数量
                        .count().as("docCount")//已预约+可预约=总;
                        .sum("reservedNumber").as("reservedNumber")
                        .sum("availableNumber").as("availableNumber"),
                //4 排序
                Aggregation.sort(Sort.Direction.DESC, "workDate"),
                //5 实现分页
                Aggregation.skip((page - 1) * limit),
                Aggregation.limit(limit)
                //1-2步类似select count（票） from 表 where hoscode=?and decode=? group by 日期 limit(?,?) desc;
        );
        //调用方法，执行得到 预约规则 的集合
        /** params1：聚合条件
         *  params2：封装实体类的class，从 mongodb 里查询出来的
         *  params3：最终返回结果的class
         */
        AggregationResults<BookingScheduleRuleVo> aggResults =
                mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        //调用 getMappedResults() 得到符合前端显示的 BookingScheduleRuleVo 集合（但此时还不能返回，因为还有一些数据里面没有）
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = aggResults.getMappedResults();

        //获取分组查询的总记录数
        Aggregation totalAgg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")
        );
        AggregationResults<BookingScheduleRuleVo> totalAggResults =
                mongoTemplate.aggregate(totalAgg, Schedule.class, BookingScheduleRuleVo.class);
        //total 是分组查询后的总记录数
        int total = totalAggResults.getMappedResults().size();

        //通过工具类获取日期对应的周几，先遍历得到 日期，再转为对应的 周几
        for (BookingScheduleRuleVo bookingScheduleRuleVo : bookingScheduleRuleVoList) {
            Date workDate = bookingScheduleRuleVo.getWorkDate();//日期
            String dayOfWeek = this.getDayOfWeek(new DateTime(workDate));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
        }

        //设置最终数据，进行返回
        Map<String, Object> result = new HashMap<>();
        result.put("bookingScheduleRuleList", bookingScheduleRuleVoList);
        result.put("total", total);

        //获取医院名称
        String hosName = hospitalService.getHospName(hoscode);
        //其他基础数据；再套一层Map是为了前端一次性读取数据
        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("hosname", hosName);
        result.put("baseMap", baseMap);

        return result;
    }

    //根据医院编号 、科室编号和工作日期，查询排班详细信息
    @Override
    public List<Schedule> getDetailSchedule(String hoscode, String depcode, String workDate) {
        //根据参数查询mongodb
        List<Schedule> scheduleList =
                scheduleRepository.findScheduleByHoscodeAndDepcodeAndWorkDate(hoscode, depcode, new DateTime(workDate).toDate());
        //把得到list集合遍历，向里面封装其他值：医院名称、科室名称、日期对应星期，方便前端显示
        scheduleList.stream().forEach(item -> {//和在 HospitalServiceImpl 中封装 医院等级 过程类似
            this.packageSchedule(item);
        });
        return scheduleList;
    }

    //获取可预约的排班数据//难点
    @Override
    public Map<String, Object> getBookingScheduleRule(int page, int limit, String hoscode, String depcode) {
        Map<String, Object> result = new HashMap<>();
        //获取预约规则
        //根据医院编号获取预约规则
        Hospital hospital = hospitalService.getByHoscode(hoscode);
        if (hospital == null) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        BookingRule bookingRule = hospital.getBookingRule();

        //获取可预约日期的数据（分页）
        IPage iPage = this.getListDate(page, limit, bookingRule);
        //当前可预约日期
        List<Date> dateList = iPage.getRecords();//分页记录列表??未

        //获取可预约日期里面科室的剩余预约数
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode)
                .and("workDate").in(dateList);

        Aggregation agg = Aggregation.newAggregation(//???
                Aggregation.match(criteria),
                Aggregation.group("workDate").first("workDate").as("workDate")
                        .count().as("docCount")
                        .sum("availableNumber").as("availableNumber")
                        .sum("reservedNumber").as("reservedNumber")
        );
        AggregationResults<BookingScheduleRuleVo> aggregateResult =
                mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);//第2进，第3出
        List<BookingScheduleRuleVo> scheduleVoList = aggregateResult.getMappedResults();//返回已映射结果的列表

        //合并数据  map集合 key日期  value预约规则和剩余数量等
        Map<Date, BookingScheduleRuleVo> scheduleVoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(scheduleVoList)) {
            scheduleVoMap = scheduleVoList.stream().
                    collect(//java 8 流
                            Collectors.toMap(BookingScheduleRuleVo::getWorkDate,
                                    BookingScheduleRuleVo -> BookingScheduleRuleVo));//???
        }

        //获取可预约排班规则
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = new ArrayList<>();
        for (int i = 0, len = dateList.size(); i < len; i++) {
            Date date = dateList.get(i);
            //从map集合根据key日期获取value值
            BookingScheduleRuleVo bookingScheduleRuleVo = scheduleVoMap.get(date);
            //如果当天没有排班医生
            if (bookingScheduleRuleVo == null) {
                bookingScheduleRuleVo = new BookingScheduleRuleVo();
                //就诊医生人数
                bookingScheduleRuleVo.setDocCount(0);
                //科室剩余预约数  -1表示无号
                bookingScheduleRuleVo.setAvailableNumber(-1);
            }
            bookingScheduleRuleVo.setWorkDate(date);
            bookingScheduleRuleVo.setWorkDateMd(date);
            //计算当前预约日期对应星期
            String dayOfWeek = this.getDayOfWeek(new DateTime(date));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);

            //最后一页最后一条记录为即将预约状态   0：正常; 1：即将放号; -1：当天已停止挂号
            if (i == len - 1 && page == iPage.getPages()) {//当前分页总页数
                bookingScheduleRuleVo.setStatus(1);
            } else {
                bookingScheduleRuleVo.setStatus(0);
            }
            //当天预约如果过了停号时间， 不能预约
            if (i == 0 && page == 1) {
                DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                if (stopTime.isBeforeNow()) {
                    //停止预约
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }
            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }

        //可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        result.put("total", iPage.getTotal());

        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        //医院名称
        baseMap.put("hosname", hospitalService.getHospName(hoscode));
        //科室
        Department department = departmentService.getDepartment(hoscode, depcode);
        //大科室名称
        baseMap.put("bigname", department.getBigname());
        //科室名称
        baseMap.put("depname", department.getDepname());
        //月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        //放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        //停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());
        result.put("baseMap", baseMap);
        return result;
    }

    //根据排班id获取排班数据(生成订单时)
    @Override
    public Schedule getScheduleId(String scheduleId) {
        System.out.println("(生成订单时)获取到的scheduleId为:"+scheduleId);//生成订单时是_id字符串，取消订单时是hosScheduleID5.7
        Schedule schedule = scheduleRepository.findById(scheduleId).get();
        return this.packageSchedule(schedule);
    }

    //根据hosScheduleID获取排班数据,因为改了原来的order_info表存的字段scheduleId，原来存的是mongodb的_id(字符串),现在是hosScheduleID 5.6
    @Override
    public Schedule getScheduleIdCancel(String scheduleId) {//(取消订单时)
        System.out.println("(取消订单时)获取到的(hos)scheduleId为:"+scheduleId);//生成订单时是_id字符串，取消订单时是hosScheduleID5.7
        Schedule schedule = scheduleRepository.getScheduleByHosScheduleId(scheduleId);
        return this.packageSchedule(schedule);
    }

    //根据排班id获取预约下单数据 最终还是去 mongodb 查询的;order模块生成订单时调用
    @Override
    public ScheduleOrderVo getScheduleOrderVo(String scheduleId) {
        ScheduleOrderVo scheduleOrderVo = new ScheduleOrderVo();
        //获取排班信息
        Schedule schedule = this.getScheduleId(scheduleId);
        //Schedule schedule = baseMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        //获取预约规则信息
        Hospital hospital = hospitalService.getByHoscode(schedule.getHoscode());
        if (hospital == null) {
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        BookingRule bookingRule = hospital.getBookingRule();
        if (bookingRule == null) {
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }

        //把获取数据设置到scheduleOrderVo
        scheduleOrderVo.setHoscode(schedule.getHoscode());
        scheduleOrderVo.setHosname(hospitalService.getHospName(schedule.getHoscode()));
        scheduleOrderVo.setDepcode(schedule.getDepcode());
        scheduleOrderVo.setDepname(departmentService.getDepName(schedule.getHoscode(), schedule.getDepcode()));
        scheduleOrderVo.setHosScheduleId(schedule.getHosScheduleId());//医院排班id!!
        scheduleOrderVo.setAvailableNumber(schedule.getAvailableNumber());
        scheduleOrderVo.setTitle(schedule.getTitle());
        scheduleOrderVo.setReserveDate(schedule.getWorkDate());
        scheduleOrderVo.setReserveTime(schedule.getWorkTime());
        scheduleOrderVo.setAmount(schedule.getAmount());

        //退号截止天数（如：就诊前一天为-1，当天为0）//还没细想 todo
        int quitDay = bookingRule.getQuitDay();
        DateTime quitTime = this.getDateTime(new DateTime(schedule.getWorkDate()).plusDays(quitDay).toDate(), bookingRule.getQuitTime());
        scheduleOrderVo.setQuitTime(quitTime.toDate());

        //预约开始时间
        DateTime startTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        scheduleOrderVo.setStartTime(startTime.toDate());

        //预约截止时间
        DateTime endTime = this.getDateTime(new DateTime().plusDays(bookingRule.getCycle()).toDate(), bookingRule.getStopTime());
        scheduleOrderVo.setEndTime(endTime.toDate());

        //当天停止挂号时间
        DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());//改进 原来重复了
        scheduleOrderVo.setStopTime(stopTime.toDate());
        return scheduleOrderVo;
    }

    //更新排班信息 用于mq
    @Override
    public void update(Schedule schedule) {
        schedule.setUpdateTime(new Date());
        scheduleRepository.save(schedule);
    }

    //获取可预约日志分页数据(只在本类使用)//?
    private IPage getListDate(int page, int limit, BookingRule bookingRule) {
        //获取当天放号时间  年 月 日 小时 分钟
        DateTime releaseTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        //获取预约周期
        Integer cycle = bookingRule.getCycle();
        //如果当天放号时间已经过去了，预约周期从后一天开始计算，周期+1
        if (releaseTime.isBeforeNow()) {
            cycle += 1;
        }
        //获取可预约所有日期，最后一天显示即将放号
        List<Date> dateList = new ArrayList<>();
        for (int i = 0; i < cycle; i++) {
            DateTime curDateTime = new DateTime().plusDays(i);//返回 已加上新增天数的 datetime
            String dateString = curDateTime.toString("yyyy-MM-dd");
            dateList.add(new DateTime(dateString).toDate());
        }
        //因为预约周期不同的，每页显示日期最多7天数据，超过7天分页
        List<Date> pageDateList = new ArrayList<>();
        int start = (page - 1) * limit;
        int end = (page - 1) * limit + limit;
        //如果可以显示数据小于7，直接显示
        if (end > dateList.size()) {//这个end就是偏移量+每页记录数  你可以理解为是7  总周期数小于7不需要分页
            end = dateList.size();
        }
        for (int i = start; i < end; i++) {
            pageDateList.add(dateList.get(i));
        }
        //如果可以显示数据大于7，进行分页
        IPage<Date> iPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, 7, dateList.size());
        iPage.setRecords(pageDateList);//设置分页记录列表?未
        return iPage;
    }

    /**
     * 将Date日期（yyyy-MM-dd HH:mm）转换为DateTime
     */
    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString = new DateTime(date).toString("yyyy-MM-dd") + " " + timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
    }

    //封装排班详情其他值 医院名称、科室名称、日期对应星期
    private Schedule packageSchedule(Schedule schedule) {
        //设置医院名称
        schedule.getParam().put("hosname", hospitalService.getHospName(schedule.getHoscode()));
        //设置科室名称
        schedule.getParam().put("depname", departmentService.getDepName(schedule.getHoscode(), schedule.getDepcode()));
        //设置日期对应星期
        schedule.getParam().put("dayOfWeek", this.getDayOfWeek(new DateTime(schedule.getWorkDate())));
        return schedule;
    }

    /**
     * 工具类，根据日期获取周几数据
     *
     * @param dateTime
     * @return
     */
    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
    }

}
