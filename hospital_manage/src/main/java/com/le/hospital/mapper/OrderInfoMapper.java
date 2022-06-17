package com.le.hospital.mapper;

import com.le.hospital.model.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

}
