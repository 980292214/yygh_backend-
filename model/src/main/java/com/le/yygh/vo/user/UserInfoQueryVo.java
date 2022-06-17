package com.le.yygh.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description="会员搜索对象")
public class UserInfoQueryVo {

    @ApiModelProperty(value = "关键字")
    private String keyword;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "认证状态")
    private Integer authStatus;

    @ApiModelProperty(value = "用户创建开始时间")
    private String createTimeBegin;//用于后台查询

    @ApiModelProperty(value = "用户创建结束时间")
    private String createTimeEnd;

}
