package com.le.hospital.controller;

import com.le.hospital.service.ApiService;
import com.le.hospital.service.HospitalService;
import com.le.hospital.util.HttpRequestHelper;
import com.le.hospital.util.Result;
import com.le.hospital.util.ResultCodeEnum;
import com.le.hospital.util.YyghException;
import com.le.hospital.util.*;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 *
 * @author qy
 *
 */
@Api(tags = "医院管理接口")
@RestController
public class HospitalController {

	@Autowired
	private HospitalService hospitalService;

	@Autowired
	private ApiService apiService;

	/**
	 * 预约下单--
	 * @param request
	 * @return
	 */
	@PostMapping("/order/submitOrder")
	public Result AgreeAccountLendProject(HttpServletRequest request, HttpServletResponse response) {
		try {
			Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());//得到调用医院接口需要的参数
//			if(!HttpRequestHelper.isSignEquals(paramMap, apiService.getSignKey())) {//判断签名
//				throw new YyghException(ResultCodeEnum.SIGN_ERROR);
//			}

			Map<String, Object> resultMap = hospitalService.submitOrder(paramMap);
			return Result.ok(resultMap);
		} catch (YyghException e) {
			return Result.fail().message(e.getMessage());
		}
	}

	/**
	 * 更新支付状态，支付成功，order模块微信支付成功后调用--
	 * @param request
	 * @return
	 */
	@PostMapping("/order/updatePayStatus")
	public Result updatePayStatus(HttpServletRequest request, HttpServletResponse response) {
		try {
			Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
			if(!HttpRequestHelper.isSignEquals(paramMap, apiService.getSignKey())) {
				throw new YyghException(ResultCodeEnum.SIGN_ERROR);
			}

			hospitalService.updatePayStatus(paramMap);
			return Result.ok();
		} catch (YyghException e) {
			return Result.fail().message(e.getMessage());
		}
	}

	/**
	 * 更新取消预约状态--
	 * @param request
	 * @return
	 */
	@PostMapping("/order/updateCancelStatus")
	public Result updateCancelStatus(HttpServletRequest request, HttpServletResponse response) {
		try {
			Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
//			if(!HttpRequestHelper.isSignEquals(paramMap, apiService.getSignKey())) {
//				throw new YyghException(ResultCodeEnum.SIGN_ERROR);
//			}

			hospitalService.updateCancelStatus(paramMap);
			return Result.ok();
		} catch (YyghException e) {
			return Result.fail().message(e.getMessage());
		}
	}
}

