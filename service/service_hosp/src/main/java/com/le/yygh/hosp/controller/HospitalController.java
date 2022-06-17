package com.le.yygh.hosp.controller;

import com.le.yygh.common.result.Result;
import com.le.yygh.hosp.service.HospitalService;
import com.le.yygh.model.hosp.Hospital;
import com.le.yygh.vo.hosp.HospitalQueryVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/hosp/hospital")
//@CrossOrigin
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    //医院列表(条件查询分页)前台使用（更新医院下线功能）
    @GetMapping("list/{page}/{limit}")
    public Result listHosp(@PathVariable Integer page,
                           @PathVariable Integer limit,
                           HospitalQueryVo hospitalQueryVo) {
          //Page 不是 mybatisplus 包的，是org.springframework.data.domain.Page
        Page<Hospital> pageModel = hospitalService.selectHospPage(page,limit,hospitalQueryVo);
        //todo nacos
        //List<Hospital> content = pageModel.getContent();//方便演示前端 response.data.content
        //long totalElements = pageModel.getTotalElements();//总记录数

        return Result.ok(pageModel);
    }

    //医院列表(条件查询分页)后台使用（医院下线也能查出来）自己加 5.3
    @GetMapping("listBack/{page}/{limit}")
    public Result listHospBack(@PathVariable Integer page,
                           @PathVariable Integer limit,
                           HospitalQueryVo hospitalQueryVo) {
        Page<Hospital> pageModel = hospitalService.selectHospPageBack(page,limit,hospitalQueryVo);
        return Result.ok(pageModel);
    }

    //更新医院上线状态
    @ApiOperation(value = "更新医院上线状态")
    @GetMapping("updateHospStatus/{id}/{status}")//PUT请求更合理
    public Result updateHospStatus(@PathVariable String id,@PathVariable Integer status) {
        hospitalService.updateStatus(id,status);
        return Result.ok();
    }

    //医院详情信息
    @ApiOperation(value = "医院详情信息")
    @GetMapping("showHospDetail/{id}")
    public Result showHospDetail(@PathVariable String id) {
        Map<String, Object> map = hospitalService.getHospById(id);
        return Result.ok(map);
    }
    //getByHoscode  todo
}
