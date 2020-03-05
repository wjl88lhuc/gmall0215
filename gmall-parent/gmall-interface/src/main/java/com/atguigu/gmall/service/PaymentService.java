package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存交易记录
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 根据out_trade_no查询
     * @param paymentInfoQuery
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    /**
     *
     * @param out_trade_no
     * @param paymentInfoUPD
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD);

    /**
     * 退款接口
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     *
     * @param orderId 订单编号
     * @param unit  金额的具体单位
     * @return
     */
    Map createNative(String orderId, String unit);

    /**
     *发送消息给订单
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * 根据第三方交易号查询交易记录
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    void closePayment(String id);
}
