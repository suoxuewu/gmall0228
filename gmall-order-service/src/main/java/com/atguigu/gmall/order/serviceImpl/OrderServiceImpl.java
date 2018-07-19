package com.atguigu.gmall.order.serviceImpl;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;



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
        return orderInfo;
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
