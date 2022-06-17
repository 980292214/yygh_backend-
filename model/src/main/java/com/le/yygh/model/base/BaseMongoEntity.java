package com.le.yygh.model.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class BaseMongoEntity implements Serializable {

    @ApiModelProperty(value = "id")
    @Id
    private String id;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @ApiModelProperty(value = "逻辑删除(1:已删除，0:未删除)")
    private Integer isDeleted;

    @ApiModelProperty(value = "其他参数")
    @Transient //被该注解标注的，将不会被录入到数据库中。只作为普通的javaBean属性
    //1.用于跨域调用时 存放数据，如 service_hosp 调用 service_cmn 获取 医院等级 todo
    //2.用于查询排班详情信息的时候 存放医院名称、科室名称、周几等信息，因为这些值在 Schedule 里没有
    private Map<String,Object> param = new HashMap<>();
}
