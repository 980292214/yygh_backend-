package com.le.yygh.vo.order;

import com.le.yygh.vo.msm.MsmVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "OrderMqVo")
public class OrderMqVo {

	@ApiModelProperty(value = "可（总）预约数")
	private Integer reservedNumber;//总预约数？

	@ApiModelProperty(value = "剩余预约数")
	private Integer availableNumber;

	@ApiModelProperty(value = "排班id")
	private String scheduleId;

	@ApiModelProperty(value = "短信实体")
	private MsmVo msmVo;

}

