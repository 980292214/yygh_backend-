package com.le.yygh.user.service.impl;

import com.le.yygh.cmn.client.DictFeignClient;
import com.le.yygh.enums.DictEnum;
import com.le.yygh.model.user.Patient;
import com.le.yygh.model.user.UserInfo;
import com.le.yygh.user.mapper.PatientMapper;
import com.le.yygh.user.mapper.UserInfoMapper;
import com.le.yygh.user.service.PatientService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientServiceImpl extends
        ServiceImpl<PatientMapper, Patient> implements PatientService {

    @Autowired
    private DictFeignClient dictFeignClient;

    /**获取就诊人列表
     * 根据用户id查询用户绑定的全部就诊人，但此时查询到的信息并不完整，证件类型和地区对应的编号（value）要转为文字名称，
     * 要显示名称的话就需要访问另一个微服务service_cmn模块（有redis缓存），根据证件类型和地区的value查询对应的名称。
     * 相当于调用另一个微服务做一个连表查询
     */
    @Override
    public List<Patient> findAllUserId(Long userId) {
        //根据userid查询所有就诊人信息列表
        QueryWrapper<Patient> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        List<Patient> patientList = baseMapper.selectList(wrapper);
        //通过远程调用，得到编码对应具体内容，查询数据字典表内容
        patientList.stream().forEach(item -> {
            //其他参数封装
            this.packPatient(item);
        });
        return patientList;
    }

    @Override
    //需要调用另一个cmn微服务
    public Patient getPatientId(Long id) {
        return this.packPatient(baseMapper.selectById(id));
    }

    //Patient对象里面其他参数封装，实际调用另一个cmn微服务
    private Patient packPatient(Patient patient) {
        //根据证件类型编码，获取证件类型具体值
        String certificatesTypeString =
                dictFeignClient.getName(DictEnum.CERTIFICATES_TYPE.getDictCode(), patient.getCertificatesType());//就诊人证件类型

        //联系人证件类型//可无？？
        String contactsCertificatesTypeString =
                dictFeignClient.getName(DictEnum.CERTIFICATES_TYPE.getDictCode(),patient.getContactsCertificatesType());

        //省
        String provinceString = dictFeignClient.getName(patient.getProvinceCode());
        //市
        String cityString = dictFeignClient.getName(patient.getCityCode());
        //区
        String districtString = dictFeignClient.getName(patient.getDistrictCode());

        patient.getParam().put("certificatesTypeString", certificatesTypeString);
        patient.getParam().put("contactsCertificatesTypeString", contactsCertificatesTypeString);
        patient.getParam().put("provinceString", provinceString);
        patient.getParam().put("cityString", cityString);
        patient.getParam().put("districtString", districtString);
        patient.getParam().put("fullAddress", provinceString + cityString + districtString + patient.getAddress());
        return patient;
    }
}
