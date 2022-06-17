package com.le.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.le.yygh.cmn.client.DictFeignClient;
import com.le.yygh.common.exception.YyghException;
import com.le.yygh.common.result.ResultCodeEnum;
import com.le.yygh.hosp.repository.HospitalRepository;
import com.le.yygh.hosp.service.HospitalService;
import com.le.yygh.hosp.service.HospitalSetService;
import com.le.yygh.model.hosp.Hospital;
import com.le.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired//跨域访问 service_cmn
    private DictFeignClient dictFeignClient;

    @Autowired
    private HospitalSetService hospitalSetService;//更新医院设置锁定功能 5.9

    //上传医院接口
    @Override
    public void save(Map<String, Object> paramMap) {
        //把参数map集合转换对象 Hospital,第一步先转成String
        String mapString = JSONObject.toJSONString(paramMap);
        Hospital hospital = JSONObject.parseObject(mapString, Hospital.class);

        //先判断医院设置状态，若已锁定，则不能上传 5.9
        String hoscode = hospital.getHoscode();
        Integer hospitalStatus = hospitalSetService.getStatusByhoscode(hoscode);//去mysql里查
        //System.out.println("获取到的医院设置状态为："+hospitalStatus);
        if (hospitalStatus==0){
            System.out.println("该医院已被锁定，无法上传医院详情信息！");
            throw new YyghException(ResultCodeEnum.HOSPITAL_LOCK);
        }

        //判断是否存在数据
        //String hoscode = hospital.getHoscode();//通过唯一索引查询
        Hospital hospitalExist = hospitalRepository.getHospitalByHoscode(hoscode);

        //如果存在，进行修改
        if (hospitalExist != null) {
            //修改时候需要把查询的数据库中原有的id传进save里，要不然还是新增+++
            hospital.setId(hospitalExist.getId());
            hospital.setStatus(hospitalExist.getStatus());//可以去掉？
            hospital.setCreateTime(hospitalExist.getCreateTime());
            hospital.setUpdateTime(new Date());
            hospital.setIsDeleted(0);
            hospitalRepository.save(hospital);
        } else {//如果不存在，进行添加
            hospital.setStatus(0);//原来是0(未上线)
            hospital.setCreateTime(new Date());
            hospital.setUpdateTime(new Date());
            hospital.setIsDeleted(0);
            hospitalRepository.save(hospital);
        }
    }

    @Override
    //实现根据医院编号查询 医院详情
    public Hospital getByHoscode(String hoscode) {
        Hospital hospital = hospitalRepository.getHospitalByHoscode(hoscode);
        return hospital;
    }

    //医院列表(条件查询分页)前台使用（更新医院下线功能）
    @Override
    public Page<Hospital> selectHospPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo) {
        hospitalQueryVo.setStatus(1);//医院下线则不查询 自己加
        //创建pageable对象
        Pageable pageable = PageRequest.of(page - 1, limit);
        //创建条件匹配器 模糊查询
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        //hospitalQueryVo转换Hospital对象
        Hospital hospital = new Hospital();
        BeanUtils.copyProperties(hospitalQueryVo, hospital);
        //创建对象
        Example<Hospital> example = Example.of(hospital, matcher);
        //调用方法实现查询
        Page<Hospital> pages = hospitalRepository.findAll(example, pageable);

        //先获取查询list集合（里面信息不全），然后遍历进行 医院等级 封装 TODO 跨域访问 service_cmn模块
        pages.getContent().stream().forEach(item -> {
            this.setHospitalHosType(item);
        });

//        //原方法
//        List<Hospital> hospitalList = pages.getContent();
//        for (Hospital origiHospital : hospitalList) {
//            //根据dictCode和value获取医院等级名称
//            String hostypeString = dictFeignClient.getName("Hostype", hospital.getHostype());
//            //查询省 市  地区
//            String provinceString = dictFeignClient.getName(hospital.getProvinceCode());
//            String cityString = dictFeignClient.getName(hospital.getCityCode());
//            String districtString = dictFeignClient.getName(hospital.getDistrictCode());
//            hospital.getParam().put("fullAddress", provinceString + cityString + districtString);
//            hospital.getParam().put("hostypeString", hostypeString);
//        }


        return pages;
    }

    //医院列表(条件查询分页)后台使用（医院下线也能查出来）
    @Override
    public Page<Hospital> selectHospPageBack(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo) {
        //hospitalQueryVo.setStatus(1);//有用
        //创建pageable对象
        Pageable pageable = PageRequest.of(page - 1, limit);
        //创建条件匹配器 模糊查询
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        //hospitalQueryVo转换Hospital对象
        Hospital hospital = new Hospital();
        BeanUtils.copyProperties(hospitalQueryVo, hospital);
        //创建对象
        Example<Hospital> example = Example.of(hospital, matcher);
        //调用方法实现查询
        Page<Hospital> pages = hospitalRepository.findAll(example, pageable);

        //先获取查询list集合（里面信息不全），然后遍历进行 医院等级 封装 TODO 跨域访问 service_cmn模块
        pages.getContent().stream().forEach(item -> {
            this.setHospitalHosType(item);
        });
        return pages;
    }

    //更新医院上线状态--
    @Override
    public void updateStatus(String id, Integer status) {
        //根据id查询医院信息
        Hospital hospital = hospitalRepository.findById(id).get();
        //设置修改的值
        hospital.setId(id);//漏了id！！
        hospital.setStatus(status);
        hospital.setUpdateTime(new Date());
        hospitalRepository.save(hospital);
    }

    //根据 id 查询医院信息
    @Override
    public Map<String, Object> getHospById(String id) {
        Map<String, Object> result = new HashMap<>();
        Hospital hospital = this.setHospitalHosType(hospitalRepository.findById(id).get());//debug 一下this
        //医院基本信息（包含医院等级）
        result.put("hospital",hospital);
        //单独处理更直观
        result.put("bookingRule", hospital.getBookingRule());
        //不需要重复返回??
        hospital.setBookingRule(null);
        return result;
    }

    //获取医院名称
    @Override
    public String getHospName(String hoscode) {
        Hospital hospital = hospitalRepository.getHospitalByHoscode(hoscode);
        if (hospital != null) {
            return hospital.getHosname();
        }
        return null;
    }

//    //根据医院名称查询(原)
//    @Override
//    public List<Hospital> findByHosname(String hosname) {
//        return hospitalRepository.findHospitalByHosnameLike(hosname);
//    }

    //根据医院名称查询//更新医院下线功能
    @Override
    public List<Hospital> findHospitalByStatusIsAndHosnameLike(Integer status,String hosname) {
        return hospitalRepository.findHospitalByStatusIsAndHosnameLike(status,hosname);//status 为1代表已上线
    }

    //根据医院编号获取医院预约挂号详情
    @Override
    public Map<String, Object> item(String hoscode) {
        Map<String, Object> result = new HashMap<>();
        //医院详情
        Hospital hospital = this.setHospitalHosType(this.getByHoscode(hoscode));
        result.put("hospital", hospital);
        //预约规则
        result.put("bookingRule", hospital.getBookingRule());
        //不需要重复返回
        hospital.setBookingRule(null);
        return result;
    }

    //获取查询list集合，遍历进行医院等级封装，方便前端显示 ;跨域调用cmn
    private Hospital setHospitalHosType(Hospital hospital) {
        //根据dictCode和value获取医院等级名称
        String hostypeString = dictFeignClient.getName("Hostype", hospital.getHostype());
        //查询省 市  地区
        String provinceString = dictFeignClient.getName(hospital.getProvinceCode());
        String cityString = dictFeignClient.getName(hospital.getCityCode());
        String districtString = dictFeignClient.getName(hospital.getDistrictCode());
        //原来的医院信息里没有以上两个值
        hospital.getParam().put("fullAddress", provinceString + cityString + districtString);
        hospital.getParam().put("hostypeString", hostypeString);
        return hospital;
    }
}
