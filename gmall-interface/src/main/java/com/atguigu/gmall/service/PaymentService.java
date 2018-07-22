package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    public void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);
    // 查询支付是否成功
    public boolean checkPayment(PaymentInfo paymentInfoQuery);

    public void savePaymentInfo(PaymentInfo paymentInfo);

    public void updataPaymentInfo(PaymentInfo paymentInfo, String out_trade_no);

    PaymentInfo getpaymentInfo(PaymentInfo paymentInfo);

    public void sendPaymentResult(PaymentInfo paymentInfo,String result);

    void closePayment(String id);
}
