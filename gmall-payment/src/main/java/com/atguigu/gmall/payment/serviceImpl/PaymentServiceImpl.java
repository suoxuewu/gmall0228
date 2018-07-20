package com.atguigu.gmall.payment.serviceImpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Autowired
    private ActiveMQUtil activeMQUtil;
    @Override
    public void updataPaymentInfo(PaymentInfo paymentInfo, String out_trade_no) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("out_trade_no",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

    @Override
    public PaymentInfo getpaymentInfo(PaymentInfo paymentInfo) {
        PaymentInfo paymentInfo1 = paymentInfoMapper.selectOne(paymentInfo);
        return paymentInfo1;
    }

    @Override
    //支付结果要知道订单编号，结果就是success或者fail
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //PAYMENT_RESULT_QUEUE支付模块通知订单系统，支付成功
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(payment_result_queue);
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId",paymentInfo.getOrderId());
            mapMessage.setString("result",result);
            producer.send(mapMessage);
            //事务不要忘了提交
            session.commit();

            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
