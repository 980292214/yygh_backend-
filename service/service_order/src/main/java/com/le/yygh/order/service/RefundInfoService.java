package com.le.yygh.order.service;

import com.le.yygh.model.order.PaymentInfo;
import com.le.yygh.model.order.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RefundInfoService extends IService<RefundInfo> {

    /**
     * 保存退款记录,首次是插入insert
     * @param paymentInfo
     */
    RefundInfo saveRefundInfo(PaymentInfo paymentInfo);

}
