package com.le.yygh.hosp.service.impl;

import com.le.yygh.common.exception.YyghException;
import com.le.yygh.common.result.ResultCodeEnum;
import com.le.yygh.hosp.mapper.HospitalSetMapper;
import com.le.yygh.hosp.service.HospitalSetService;
import com.le.yygh.model.hosp.HospitalSet;
import com.le.yygh.vo.order.SignInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.Query;

@Service
public class HospitalSetServiceImpl extends ServiceImpl<HospitalSetMapper, HospitalSet> implements HospitalSetService {

    //2 根据传递过来医院编码，查询数据库，查询签名
    @Override
    public String getSignKey(String hoscode) {
        QueryWrapper<HospitalSet> wrapper = new QueryWrapper<>();
        wrapper.eq("hoscode",hoscode);
        HospitalSet hospitalSet = baseMapper.selectOne(wrapper);//baseMapper 就是hospitalSetMapper
        return hospitalSet.getSignKey();
    }

    //根据医院编号获取医院签名信息；order模块生成订单时调用
    @Override
    public SignInfoVo getSignInfoVo(String hoscode) {
        QueryWrapper<HospitalSet> wrapper = new QueryWrapper<>();
        wrapper.eq("hoscode",hoscode);
        HospitalSet hospitalSet = baseMapper.selectOne(wrapper);
        if(null == hospitalSet) {
            throw new YyghException(ResultCodeEnum.HOSPITAL_OPEN);
        }
        SignInfoVo signInfoVo = new SignInfoVo();
        signInfoVo.setApiUrl(hospitalSet.getApiUrl());//查的是yygh_hosp的表
        signInfoVo.setSignKey(hospitalSet.getSignKey());
        System.out.println("yygh_hosp的表:hospitalSet.getApiUrl() = "+hospitalSet.getApiUrl());
        return signInfoVo;
    }

    //更新医院设置锁定功能 自己加5.9
    public Integer getStatusByhoscode(String hoscode){
        QueryWrapper<HospitalSet> wrapper = new QueryWrapper<>();
        wrapper.eq("hoscode",hoscode);
        HospitalSet hospitalSet = baseMapper.selectOne(wrapper);
        Integer status = hospitalSet.getStatus();
        return status;
    }

}
