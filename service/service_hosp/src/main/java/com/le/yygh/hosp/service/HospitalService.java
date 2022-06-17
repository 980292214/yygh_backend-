package com.le.yygh.hosp.service;

import com.le.yygh.model.hosp.Hospital;
import com.le.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface HospitalService {
    //上传医院接口
    void save(Map<String, Object> paramMap);

    //实现根据医院编号查询
    Hospital getByHoscode(String hoscode);

    //医院列表(条件查询分页)前台使用（有医院下线功能）
    Page<Hospital> selectHospPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo);

    //医院列表(条件查询分页)后台使用（医院下线也能查出来）
    Page<Hospital> selectHospPageBack(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo);

    //更新医院上线状态
    void updateStatus(String id, Integer status);

    //医院详情信息
    Map<String, Object> getHospById(String id);

    //获取医院名称
    String getHospName(String hoscode);

//    //根据医院名称查询//后台使用
//    List<Hospital> findByHosname(String hosname);

    //根据医院名称查询//前台使用，更新医院下线功能5.3
    List<Hospital> findHospitalByStatusIsAndHosnameLike(Integer status, String hosname);

    //根据医院编号获取医院预约挂号详情
    Map<String, Object> item(String hoscode);
}
