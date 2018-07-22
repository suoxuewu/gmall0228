package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.OrderStatus;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserAddressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

//    @Reference
//    private UserInfoService userInfoService;
   @Reference
   CartService cartService;

   @Reference
   private UserAddressService userAddressService;

   @Reference
   private OrderService orderService;

   @RequestMapping(value="orderSplit",method = RequestMethod.POST)
   @ResponseBody
   public String orderSplit(HttpServletRequest request){
       //根据orderId，进行拆单
       String orderId = request.getParameter("orderId");
       // 返回的Json数据[{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
       //[{"wareId":"1","skuId":["2","2"]},{}]
       String wareSkuMap = request.getParameter("wareSkuMap");
       //根据orderId进行拆单
       List<OrderInfo> orderInfo = null;
       try {
           orderInfo = orderService.orderSplit(orderId,wareSkuMap);
       } catch (InvocationTargetException e) {
           e.printStackTrace();
       } catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       List<Map> orderDeatilList = new ArrayList<>();
       for (OrderInfo info : orderInfo) {
           //从订单中获取字订单
          Map map =  orderService.initWareOrder(info);
          orderDeatilList.add(map);
       }
       return JSON.toJSONString(orderDeatilList);
   }


    //表单提交submitOrder
    @RequestMapping(value = "submitOrder",method = RequestMethod.POST)
    @LoginRequire
    public String submitOrder(HttpServletRequest request,OrderInfo orderInfo){
        String userId = (String) request.getAttribute("userId");
        String tradeNo = request.getParameter("tradeNo");
        //防止重复提交
//        boolean falg = orderService.checkTradeCode(tradeNo, userId);
//        if(!falg){
//            request.setAttribute("errMsg","订单不能重复提交，你想干啥？");
//            return "tradeFail";
//        }
        //验证一下库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
//        for (OrderDetail orderDetail : orderDetailList) {
//            boolean b = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
//            if(!b){
//                request.setAttribute("errMsg","库存不足，请您去联系管理员奥。。。。");
//                return "tradeFail";
//            }
//        }
        orderInfo.setUserId(userId);
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.sumTotalAmount();
        orderInfo.setTotalAmount(orderInfo.getTotalAmount());
        String orderId1 = orderService.saveOrder(orderInfo);
        //System.out.println("测试的ordeID"+orderId1);
        //删除流水号
        //orderService.delTradeNo(userId);
        //重定向支付页面,需要写到orderid
        return "redirect://payment.gmall.com/index?orderId="+orderId1;
    }


    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request, Model model){
        String userId = (String) request.getAttribute("userId");
        //得到选中的购物车列表
        List<CartInfo> cartCheckedList  = cartService.getCartCheckedList(userId);
        //订单信息集合
        List<OrderDetail> orderDetailList = new ArrayList<>(cartCheckedList.size());
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail od = new OrderDetail();
            od.setSkuId(cartInfo.getSkuId());
            od.setImgUrl(cartInfo.getImgUrl());
            od.setSkuName(cartInfo.getSkuName());
            od.setSkuNum(cartInfo.getSkuNum());
            od.setOrderPrice(cartInfo.getCartPrice());
            orderDetailList.add(od);
        }
        model.addAttribute("orderDetailList",orderDetailList);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        model.addAttribute("totalAmount",orderInfo.getTotalAmount());
        //保存流水号给前台
        String tradeNo = orderService.getTradeNo(userId);
        model.addAttribute("tradeCode",tradeNo);

        //得到用户地址
        List<UserAddress> userAddressList = userAddressService.getUserAddressList(userId);
        model.addAttribute("userAddressList",userAddressList);
        return "trade";
    }

    @RequestMapping(value="index")
    public String index(){
        return "trade";
    }
}
