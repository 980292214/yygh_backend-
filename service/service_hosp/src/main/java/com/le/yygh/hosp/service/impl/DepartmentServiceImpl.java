package com.le.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.le.yygh.common.exception.YyghException;
import com.le.yygh.common.result.ResultCodeEnum;
import com.le.yygh.hosp.repository.DepartmentRepository;
import com.le.yygh.hosp.service.DepartmentService;
import com.le.yygh.hosp.service.HospitalSetService;
import com.le.yygh.model.hosp.Department;
import com.le.yygh.vo.hosp.DepartmentQueryVo;
import com.le.yygh.vo.hosp.DepartmentVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private HospitalSetService hospitalSetService;//更新医院设置锁定功能 5.9

    //上传科室接口
    @Override
    public void save(Map<String, Object> paramMap) {
        //paramMap 转换department对象
        String paramMapString = JSONObject.toJSONString(paramMap);
        Department department = JSONObject.parseObject(paramMapString,Department.class);

        //先判断医院设置状态，若已锁定，则不能上传 5.9
        String hoscode = department.getHoscode();
        Integer hospitalStatus = hospitalSetService.getStatusByhoscode(hoscode);//去mysql里查
        //System.out.println("获取到的医院设置状态为："+hospitalStatus);
        if (hospitalStatus==0){
            System.out.println("该医院已被锁定，无法上传科室信息！");
            throw new YyghException(ResultCodeEnum.HOSPITAL_LOCK);
        }

        //根据医院编号 和 科室编号查询
        Department departmentExist = departmentRepository.
                getDepartmentByHoscodeAndDepcode(department.getHoscode(),department.getDepcode());

        //判断
        if(departmentExist!=null) {
            department.setId(departmentExist.getId());//
            department.setCreateTime(departmentExist.getCreateTime());
            //department.setCreateTime(new Date());//不能把数据库里日期改成字符串
            department.setUpdateTime(new Date());
            department.setIsDeleted(0);
            departmentRepository.save(department);
            //departmentRepository.save(departmentExist);
        } else {
            department.setCreateTime(new Date());
            department.setUpdateTime(new Date());
            department.setIsDeleted(0);
            departmentRepository.save(department);
        }
    }

    @Override
    //配合 MongoRepository 文档看
    public Page<Department> findPageDepartment(int page, int limit, DepartmentQueryVo departmentQueryVo) {//todo
        // 创建Pageable对象，设置当前页和每页记录数
        //0是第一页
        Pageable pageable = PageRequest.of(page-1,limit);
        // 创建Example对象
        Department department = new Department();
        //departmentQueryVo --> department
        //亲测直接用JSONObject得到一个Department 然后操作 不用DepartmentQueryVo？？？
        BeanUtils.copyProperties(departmentQueryVo,department);
        department.setIsDeleted(0);

        ExampleMatcher matcher = ExampleMatcher.matching()
            .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
            .withIgnoreCase(true);
        Example<Department> example = Example.of(department,matcher);

        //Page 是这个包 package org.springframework.data.domain;
        Page<Department> all = departmentRepository.findAll(example, pageable);
        return all;
    }

    //删除科室接口
    @Override
    public void remove(String hoscode, String depcode) {
        //根据医院编号 和 科室编号查询
        Department department = departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
        if(department != null) {
            //调用方法删除
            departmentRepository.deleteById(department.getId());
        }
    }

    //根据医院编号，查询医院所有科室列表
    @Override
    public List<DepartmentVo> findDeptTree(String hoscode) {
        //创建list集合，用于最终数据封装(方便前端显示)
        List<DepartmentVo> result = new ArrayList<>();

        //根据医院编号，查询医院所有科室信息
        Department departmentQuery = new Department();
        departmentQuery.setHoscode(hoscode);
        Example example = Example.of(departmentQuery);//只有一个参数?
        //所有科室列表 departmentList,此时 departmentList 的数据格式不满足前端显示要求，所以要继续封装(截图P92)
        //非关系数据库，封装起来不太习惯
        List<Department> departmentList = departmentRepository.findAll(example);


        /**原方法：遍历 departmentList ，获取每个大科室的 bigcode，然后按照 bigcode 分组
         * java8 新特性，stream流 针对集合或数组进行操作，但不改变原来存放的值
         */
        //根据大科室编号  bigcode 分组，目的是获取每个大科室里面下级子科室
        //返回的键 是大科室 编号，值List里包含了所有部门信息（看表中的数据）
        Map<String, List<Department>> deparmentMap =
                departmentList.stream().collect(Collectors.groupingBy(Department::getBigcode));
        //遍历map集合 deparmentMap
        for(Map.Entry<String,List<Department>> entry : deparmentMap.entrySet()) {//细看不懂?
            //大科室编号
            String bigcode = entry.getKey();
            //大科室编号对应的全局数据
            List<Department> tempDeparmentList = entry.getValue();
            //封装大科室
            DepartmentVo bigDepartmentVo = new DepartmentVo();
            bigDepartmentVo.setDepcode(bigcode);//大科室编号
            //tempDeparmentList.get(0) 就是分组后的第一条数据，通过它直接获取大科室名称
            bigDepartmentVo.setDepname(tempDeparmentList.get(0).getBigname());

            //封装小科室
            List<DepartmentVo> children = new ArrayList<>();
            for(Department department: tempDeparmentList) {
                DepartmentVo smallDepartmentVo =  new DepartmentVo();
                smallDepartmentVo.setDepcode(department.getDepcode());
                smallDepartmentVo.setDepname(department.getDepname());
                //封装到list集合
                children.add(smallDepartmentVo);
            }
            //把小科室list集合放到大科室children里面
            bigDepartmentVo.setChildren(children);
            //放到最终result里面
            result.add(bigDepartmentVo);
        }
        //返回符合前端显示的数据格式 DepartmentVo
        return result;
    }

    //根据科室编号，和医院编号，查询科室名称
    @Override
    public String getDepName(String hoscode, String depcode) {
        Department department = departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
        if(department != null) {
            return department.getDepname();
        }
        return null;
    }

    @Override
    public Department getDepartment(String hoscode, String depcode) {
        return departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
    }

}
