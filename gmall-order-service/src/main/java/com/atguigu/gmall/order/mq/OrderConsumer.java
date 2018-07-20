package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {
    @Reference
    OrderService orderService;
    //实际上就是根据MessageListener监听器，不断的去扫描，r
    //如果有匹配的name ,则进行消费
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        System.out.println("orderId="+orderId+"-------"+"result="+result);
        if("success".equals(result)){
            //更新支付状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            //发送减库的通知
            //先发送消息队列,procerduce  name是ORDER_RESULT_QUEUE
            orderService.sendOrderStatus(orderId);
            //更新订单的进度，变成等待发货
            orderService.updateOrderStatus(orderId,ProcessStatus.WAITING_DELEVER);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
        }
    }
    //消费库存传过来的SKU_DEDUCT_QUEUE，更改订单状态
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        /*状态： ‘DEDUCTED’  (已减库存)
状态：  ‘OUT_OF_STOCK’  (库存超卖 )
*/
        if("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(orderId,ProcessStatus.DELEVERED);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.STOCK_EXCEPTION);
        }
    }

}
