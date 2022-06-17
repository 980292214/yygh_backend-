package com.le.yygh.hosp.repository;

import com.le.yygh.model.hosp.Hospital;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends MongoRepository<Hospital,String> {
    //判断是否存在数据
    Hospital getHospitalByHoscode(String hoscode);//方法不用自己实现，因为继承了 MongoRepository todo

//    //根据医院名称查询//原前台使用（后台用不上）
//    List<Hospital> findHospitalByHosnameLike(String hosname);

    //更新医院下线功能//前台使用//5.3测试已通过
    List<Hospital> findHospitalByStatusIsAndHosnameLike(Integer status,String hosname);
}
