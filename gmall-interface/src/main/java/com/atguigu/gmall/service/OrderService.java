package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public interface OrderService {
    public String getTradeNo(String userId);
    public  boolean checkTradeCode(String tradeNo,String userId);
    public void delTradeNo(String userId);
    public boolean checkStock(String skuId,Integer skuNum);
    public String saveOrder(OrderInfo orderInfo);
    OrderInfo getOrderInfo(String orderId);
    public List<OrderInfo> getExpireOrderList();


    void updateOrderStatus(String orderId, ProcessStatus paid);

    void sendOrderStatus(String orderId);

    void execExpireOrder(OrderInfo orderInfo);

    Map initWareOrder(OrderInfo orderInfo);

    List<OrderInfo> orderSplit(String orderId, String wareSkuMap) throws InvocationTargetException, IllegalAccessException;
}
