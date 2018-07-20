package com.atguigu.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {
    public static void main(String[] args) throws JMSException {
        ConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory("tcp://192.168.15.129:61616");
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session =  connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue("Atguigu");
        MessageProducer producer = session.createProducer(queue);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("heool wode er zi ");
        producer.send(activeMQTextMessage);
        producer.close();
        connection.close();
        session.close();
        //如果有事务记得Session.commit();
    }

}
