package com.le.yygh.hosp.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.le.yygh.common.result.Result;
import com.le.yygh.common.utils.MD5;
import com.le.yygh.hosp.service.HospitalSetService;
import com.le.yygh.model.hosp.HospitalSet;
import com.le.yygh.vo.hosp.HospitalSetQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

/**
 * @author 乐
 * @version 1.0
 */
//在Swagger页面上显示中文提示;修饰整个类，描述Controller的作用
@Api(tags = "医院设置管理")
@RestController
//@CrossOrigin 有网关了
@RequestMapping("/admin/hosp/hospitalSet")
public class HospitalSetController {
    @Autowired
    private HospitalSetService hospitalSetService;

    //1 查询医院设置表所有信息
    @ApiOperation(value = "获取所有医院设置")
    @GetMapping("findAll")
    public Result findAllHospitalSet() {
        //调用service的方法
        List<HospitalSet> list = hospitalSetService.list();
        return Result.ok(list);
    }

    //2 逻辑删除医院设置
    @ApiOperation(value = "逻辑删除医院设置")
    @DeleteMapping("{id}")
    public Result removeHospSet(@PathVariable Long id) {
        boolean flag = hospitalSetService.removeById(id);
        return flag ? Result.ok() : Result.fail();
    }

    //3 条件查询带分页
    @PostMapping("findPageHospSet/{current}/{limit}")
    public Result findPageHospSet(@PathVariable long current,
                                  @PathVariable long limit,
                                  @RequestBody(required = false) HospitalSetQueryVo hospitalSetQueryVo) {
        //构建条件
        QueryWrapper<HospitalSet> wrapper = new QueryWrapper<>();
        String hosname = hospitalSetQueryVo.getHosname();//医院名称
        String hoscode = hospitalSetQueryVo.getHoscode();//医院编号
        if (!StringUtils.isEmpty(hosname)) {
            wrapper.like("hosname", hosname);
        }
        if (!StringUtils.isEmpty(hoscode)) {
            wrapper.eq("hoscode", hoscode);
        }
        //判断数据库是否有匹配的值+++//能否再改进，两个条件是or？？？
        if (hospitalSetService.list(wrapper).size() == 0) {
            return Result.fail();
        }

        //创建page对象，传递当前页，每页记录数
        Page<HospitalSet> page = new Page<>(current, limit);
        //调用方法实现分页查询
        IPage<HospitalSet> pageHospitalSet = hospitalSetService.page(page, wrapper);
        //返回结果
        return Result.ok(pageHospitalSet);
    }

    //4 添加医院设置
    @PostMapping("saveHospitalSet")
    public Result saveHospitalSet(@RequestBody HospitalSet hospitalSet) {
        //设置状态 1 可用， 0 不可用
        hospitalSet.setStatus(1);
        //签名秘钥
        Random random = new Random();
        hospitalSet.setSignKey(MD5.encrypt(System.currentTimeMillis() + "" + random.nextInt(1000)));
        //调用service
        boolean save = hospitalSetService.save(hospitalSet);
        return save ? Result.ok() : Result.fail();
    }

    //5 根据id获取医院设置
    @GetMapping("getHospSet/{id}")
    public Result getHospSet(@PathVariable Long id) {
//        try {
//            //模拟异常
//            int a = 1/0;
//        }catch (Exception e) {
//            throw new YyghException("失败2222",201);
//        }

        HospitalSet hospitalSet = hospitalSetService.getById(id);
        //若找不到+++
        if (StringUtils.isEmpty(hospitalSet))
            return Result.fail();
        return Result.ok(hospitalSet);
    }

    //6 根据 id 修改医院设置
    @PostMapping("updateHospitalSet")
    public Result updateHospitalSet(@RequestBody HospitalSet hospitalSet) {
        boolean flag = hospitalSetService.updateById(hospitalSet);
        return flag ? Result.ok() : Result.fail();
    }

    //7 批量删除医院设置
    @DeleteMapping("batchRemove")
    public Result batchRemoveHospitalSet(@RequestBody List<Long> idList) {
        boolean flag = hospitalSetService.removeByIds(idList);
        return flag ? Result.ok() : Result.fail();
    }

    //8 医院设置锁定和解锁
    @PutMapping("lockHospitalSet/{id}/{status}")
    public Result lockHospitalSet(@PathVariable Long id,
                                  @PathVariable Integer status) {
        //先根据id查询医院设置信息
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        //若找不到
        if (StringUtils.isEmpty(hospitalSet))
            return Result.fail();
        //找到后设置状态
        hospitalSet.setStatus(status);
        //调用方法
        hospitalSetService.updateById(hospitalSet);
        return Result.ok();
    }

    //9 发送签名秘钥
    @PutMapping("sendKey/{id}")
    public Result SendlockHospitalSet(@PathVariable Long id) {
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        String signKey = hospitalSet.getSignKey();
        String hoscode = hospitalSet.getHoscode();
        //发送短信
        return Result.ok();
    }

//    //更新医院设置锁定功能 自己加 未5.10
//    @GetMapping("getStatusByhoscode/{hoscode}")
//    public Integer getStatusByhoscode(@PathVariable String hoscode){
//        Integer statusByhoscode = hospitalSetService.getStatusByhoscode(hoscode);
//        return statusByhoscode;//优化统一返回结果??
//    }
}
