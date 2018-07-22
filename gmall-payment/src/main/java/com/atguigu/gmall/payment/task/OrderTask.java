package com.atguigu.gmall.payment.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

//启动轮训
@EnableScheduling
@Component
public class OrderTask {
    //添加定时器
    @Reference
    OrderService orderService;
    @Scheduled(cron = "0/20 * * * * ?")
    public void checkOrder(){
        //获取过期的订单
        List<OrderInfo> expireOrderList = orderService.getExpireOrderList();
        //循环订单，更新状态
        for (OrderInfo orderInfo : expireOrderList) {
            orderService.execExpireOrder(orderInfo);
        }
    }
}
