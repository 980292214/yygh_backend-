package com.le.yygh.hosp.controller.api;

import com.le.yygh.common.result.Result;
import com.le.yygh.hosp.service.DepartmentService;
import com.le.yygh.hosp.service.HospitalService;
import com.le.yygh.hosp.service.HospitalSetService;
import com.le.yygh.hosp.service.ScheduleService;
import com.le.yygh.model.hosp.Hospital;
import com.le.yygh.model.hosp.Schedule;
import com.le.yygh.vo.hosp.DepartmentVo;
import com.le.yygh.vo.hosp.HospitalQueryVo;
import com.le.yygh.vo.hosp.ScheduleOrderVo;
import com.le.yygh.vo.order.SignInfoVo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 处理 前台用户页面 传来的请求
 */
@RestController
@RequestMapping("/api/hosp/hospital")
public class HospApiController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private HospitalSetService hospitalSetService;

    //前台使用（有医院下线功能）
    @ApiOperation(value = "查询医院列表")//访问cmn
    @GetMapping("findHospList/{page}/{limit}")
    public Result findHospList(@PathVariable Integer page,
                               @PathVariable Integer limit,
                               HospitalQueryVo hospitalQueryVo) {
        Page<Hospital> hospitals = hospitalService.selectHospPage(page, limit, hospitalQueryVo);
        return Result.ok(hospitals);
    }

    @ApiOperation(value = "根据医院名称（模糊）查询,前台使用，更新医院下线功能5.3")
    @GetMapping("findHospitalByStatusIsAndHosnameLike/{hosname}")
    public Result findHospitalByStatusIsAndHosnameLike(@PathVariable String hosname) {
        List<Hospital> list = hospitalService.findHospitalByStatusIsAndHosnameLike(1,hosname);//status 为1代表已上线
        return Result.ok(list);
    }

//    @ApiOperation(value = "根据医院名称（模糊）查询;原来的前台使用,下线了的也查出来了5.3不用")
//    @GetMapping("findByHosName/{hosname}")
//    public Result findByHosname(@PathVariable String hosname) {
//        List<Hospital> list = hospitalService.findByHosname(hosname);
//        return Result.ok(list);
//    }

    @ApiOperation(value = "根据医院编号获取科室")
    @GetMapping("department/{hoscode}")
    public Result index(@PathVariable String hoscode) {
        List<DepartmentVo> list = departmentService.findDeptTree(hoscode);
        return Result.ok(list);
    }

    @ApiOperation(value = "根据医院编号获取医院预约挂号详情")//访问cmn
    @GetMapping("findHospDetail/{hoscode}")
    public Result item(@PathVariable String hoscode) {
        Map<String, Object> map = hospitalService.item(hoscode);
        return Result.ok(map);
    }

    @ApiOperation(value = "获取可预约排班数据")//难点
    @GetMapping("auth/getBookingScheduleRule/{page}/{limit}/{hoscode}/{depcode}")
    public Result getBookingSchedule(
            @ApiParam(name = "page", value = "当前页码", required = true)
            @PathVariable Integer page,
            @ApiParam(name = "limit", value = "每页记录数", required = true)
            @PathVariable Integer limit,
            @ApiParam(name = "hoscode", value = "医院code", required = true)
            @PathVariable String hoscode,
            @ApiParam(name = "depcode", value = "科室code", required = true)
            @PathVariable String depcode) {
        return Result.ok(scheduleService.getBookingScheduleRule(page, limit, hoscode, depcode));
    }

    @ApiOperation(value = "获取排班数据")
    @GetMapping("auth/findScheduleList/{hoscode}/{depcode}/{workDate}")
    public Result findScheduleList(
            @ApiParam(name = "hoscode", value = "医院code", required = true)
            @PathVariable String hoscode,
            @ApiParam(name = "depcode", value = "科室code", required = true)
            @PathVariable String depcode,
            @ApiParam(name = "workDate", value = "排班日期", required = true)
            @PathVariable String workDate) {
        return Result.ok(scheduleService.getDetailSchedule(hoscode, depcode, workDate));
    }

    @ApiOperation(value = "根据排班id获取排班数据")
    @GetMapping("getSchedule/{scheduleId}")
    public Result getSchedule(@PathVariable String scheduleId) {
        Schedule schedule = scheduleService.getScheduleId(scheduleId);
        return Result.ok(schedule);
    }

    @ApiOperation(value = "根据排班id获取预约下单数据")
    @GetMapping("inner/getScheduleOrderVo/{scheduleId}")//order模块生成/取消订单时调用
    public ScheduleOrderVo getScheduleOrderVo(
            @ApiParam(name = "scheduleId", value = "排班id", required = true)
            @PathVariable("scheduleId") String scheduleId) {
        return scheduleService.getScheduleOrderVo(scheduleId);
    }

    @ApiOperation(value = "根据医院编号获取医院签名信息")//order模块生成订单时调用
    @GetMapping("inner/getSignInfoVo/{hoscode}")
    public SignInfoVo getSignInfoVo(
            @ApiParam(name = "hoscode", value = "医院code", required = true)
            @PathVariable("hoscode") String hoscode) {
        return hospitalSetService.getSignInfoVo(hoscode);
    }

}
