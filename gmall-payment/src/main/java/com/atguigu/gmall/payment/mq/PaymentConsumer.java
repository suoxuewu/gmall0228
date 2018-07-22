package com.atguigu.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {
    @Reference
    PaymentService paymentService;
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage){
        try {
            String outTradeNo = mapMessage.getString("outTradeNo");
            int delaySec = mapMessage.getInt("delaySec");
            int checkCount = mapMessage.getInt("checkCount");
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOutTradeNo(outTradeNo);
            //检查是否付款
            boolean result = paymentService.checkPayment(paymentInfo);
            if(!result&&checkCount!=0){
                System.out.println("检查的次数="+checkCount);
                paymentService.sendPaymentResult(paymentInfo,outTradeNo);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }


    }
}
