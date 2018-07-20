package com.atguigu.gmall.service;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;

public interface OrderService {
    public String getTradeNo(String userId);
    public  boolean checkTradeCode(String tradeNo,String userId);
    public void delTradeNo(String userId);
    public boolean checkStock(String skuId,Integer skuNum);
    public String saveOrder(OrderInfo orderInfo);
    OrderInfo getOrderInfo(String orderId);


    void updateOrderStatus(String orderId, ProcessStatus paid);

    void sendOrderStatus(String orderId);
}
