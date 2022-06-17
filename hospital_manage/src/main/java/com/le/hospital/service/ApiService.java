package com.le.hospital.service;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.Map;

public interface ApiService {

    String getHoscode();

    String getSignKey();

    JSONObject getHospital();

    boolean saveHospital(String data);

    Map<String, Object> findDepartment(int pageNum, int pageSize);

    boolean saveDepartment(String data);

    boolean removeDepartment(String depcode);

    Map<String, Object> findSchedule(int pageNum, int pageSize);

    //先保存到h_manege，然后发给yygh_hosp--
    boolean saveSchedule(String data);

    boolean removeSchedule(String hosScheduleId);

    void  saveBatchHospital() throws IOException;
}
