package com.le.yygh.hosp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.le.yygh.model.hosp.HospitalSet;
import com.le.yygh.vo.order.SignInfoVo;

/**
 * @author 乐
 * @version 1.0
 */
public interface HospitalSetService extends IService<HospitalSet> {
    //2 根据传递过来医院编码，查询数据库，查询签名
    String getSignKey(String hoscode);

    //根据医院编号获取医院签名信息
    SignInfoVo getSignInfoVo(String hoscode);

    //更新医院设置锁定功能 自己加5.9
    Integer getStatusByhoscode(String hoscode);
}
