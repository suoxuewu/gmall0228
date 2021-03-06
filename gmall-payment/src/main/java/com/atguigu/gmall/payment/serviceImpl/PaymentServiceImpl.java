package com.atguigu.gmall.payment.serviceImpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private AlipayClient alipayClient;
    // 验证支付宝是否支付成功

    @Override
    //在二维码付款的时候发送延迟队列
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",outTradeNo);
            activeMQMapMessage.setInt("delaySec",delaySec);
            activeMQMapMessage.setInt("checkCount",checkCount);
            //设置延迟时间
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_CRON,delaySec*1000);
            producer.send(activeMQMapMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param paymentInfoQuery 有out_trade_no
     * @return
     */
    public boolean checkPayment(PaymentInfo paymentInfoQuery){
        // 根据传入的对象查找paymengInfo 对象
        PaymentInfo paymentInfo = getpaymentInfo(paymentInfoQuery);

        if (paymentInfo.getPaymentStatus()== PaymentStatus.ClOSED || paymentInfo.getPaymentStatus()== PaymentStatus.PAID){
            //  说明该交易成功！
            return true;
        }

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // out_trade_no:ATGUIGU1532006223101012 orderId = 53 的
        request.setBizContent("{" +
                "\"out_trade_no\":\""+paymentInfo.getOutTradeNo()+"\"" +
                "  }");
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            // TRADE_SUCCESS=支付成功，TRADE_FINISHED=支付成功不可退款！
            if ("TRADE_SUCCESS".equals(response.getTradeStatus()) ||
                    "TRADE_FINISHED".equals(response.getTradeStatus())){
                // 修改订单-支付状态，
                paymentInfo.setPaymentStatus(PaymentStatus.PAID);
                // 修改时间
                paymentInfo.setCreateTime(new Date());
                // 更新数据状态
                updataPaymentInfo(paymentInfo,paymentInfo.getOutTradeNo());
                // 发送消息给订单。修改订单状态。 url:payment.gmall.com/sendPaymentResult?
                //orderId=53&result=success
                sendPaymentResult(paymentInfo,"success");
                System.out.println("调用成功");
                return  true;
            }else{
                return false;
            }
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

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

    @Override
    public void closePayment(String id) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",id);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }
}
