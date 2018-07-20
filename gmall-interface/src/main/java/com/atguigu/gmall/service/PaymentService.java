package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    public void savePaymentInfo(PaymentInfo paymentInfo);

    public void updataPaymentInfo(PaymentInfo paymentInfo, String out_trade_no);

    PaymentInfo getpaymentInfo(PaymentInfo paymentInfo);

    public void sendPaymentResult(PaymentInfo paymentInfo,String result);
}
