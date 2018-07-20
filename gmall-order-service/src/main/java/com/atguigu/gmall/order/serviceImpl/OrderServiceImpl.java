package com.atguigu.gmall.order.serviceImpl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ActiveMQUtil activeMQUtil;



    @Override
    //@Transactional
    public String saveOrder(OrderInfo orderInfo) {
        //设置创建时间
        orderInfo.setCreateTime(new Date());
        //设置失效时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //第三方支付编号
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfoMapper.insertSelective(orderInfo);
        //插曲订单详情
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        //返回订单编号
        return orderInfo.getId();
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(paid);
        orderInfo.setOrderStatus(paid.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        //先发送消息队列,封装仓库需要的数据
        Connection connection = activeMQUtil.getConnection();
        Session session = null;
        String orderJson = initWareOrder(orderId);
        try {
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
           Queue order_result_queue =  session.createQueue("ORDER_RESULT_QUEUE");
           MessageProducer producer =  session.createProducer(order_result_queue);
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(orderJson);
            producer.send(activeMQTextMessage);
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    private String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map =  initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    private Map initWareOrder(OrderInfo orderInfo) {
        //这个方法用来拼接字符串,初始化仓库信息
        Map map = new HashMap();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        //付款方式
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());
        //封装ouderDeatailList
        ArrayList<Object> arrayList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map mapDetail = new HashMap();
            mapDetail.put("skuId",orderDetail.getSkuId());
            mapDetail.put("skuNum",orderDetail.getSkuNum());
            mapDetail.put("skuName",orderDetail.getSkuName());
            arrayList.add(mapDetail);
        }
        map.put("details",arrayList);
        return map;
    }


    /*生成流水号,放到redis中
    * */
    public String getTradeNo(String userId){
        String tradeNoKey = "user:"+userId+":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        String tradeNo = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey,10*60,tradeNo);
        return tradeNo;
    }
    //检查流水号
    public  boolean checkTradeCode(String tradeNo,String userId){
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:"+userId+":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        if(tradeCode!=null&&!"".equals(tradeCode)){
            if(tradeCode.equals(tradeNo)){
                return true;
            }else {
                return false;
            }
        }
        return false;
    }
    //删除redis中的tradeNo
    public void delTradeNo(String userId){
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:"+userId+":tradeCode";
        jedis.del(tradeNoKey);
    }
    public boolean checkStock(String skuId,Integer skuNum){
        //远程调用仓库
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if("1".equals(result)){
            return true;
        }else {
            return false;
        }
    }
}
