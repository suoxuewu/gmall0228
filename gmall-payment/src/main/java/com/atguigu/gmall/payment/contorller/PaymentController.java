package com.atguigu.gmall.payment.contorller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {
    @Autowired
    AlipayClient alipayClient;
    @Reference
    OrderService orderService;
    @Reference
    PaymentService paymentService;
    //return "redirect://payment.gmall.com/index?orderId="+orderInfo.getId();

    @RequestMapping(value="queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request){
        //先获取订单编号
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        PaymentInfo info = paymentService.getpaymentInfo(paymentInfo);
        boolean b = paymentService.checkPayment(info);
        return "result="+b;
    }

    @RequestMapping(value = "sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,@RequestParam("result")String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sent payment result";
    }


    @RequestMapping(value = "index")
    @LoginRequire
    public String index(HttpServletRequest request, Model model){
        // 取得订单id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        model.addAttribute("orderId",orderId);
        model.addAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }
    /*
    * 分析 ：
1、通过orderId取得订单信息
2、组合对应的支付信息保存到数据库。
3、组合需要传给支付宝的参数。
4、根据返回的表单html，传给浏览器。
*/
    @RequestMapping(value = "/alipay/submit",method = RequestMethod.POST)
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());
        //保存支付信息
        paymentService.savePaymentInfo(paymentInfo);
        //保存信息
        //支付宝参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        //同步的Url
        alipayRequest.setReturnUrl(AlipayConfig.return_order_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
        Map<String,Object> bizContnetMap = new HashMap<>();
        bizContnetMap.put("out_trade_no",paymentInfo.getOutTradeNo());
        bizContnetMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        bizContnetMap.put("subject",paymentInfo.getSubject());
        bizContnetMap.put("total_amount",paymentInfo.getTotalAmount());
        //将map编程json
        String Json = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(Json);
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),14,4);
        return form;
    }
    //同步回调
    @RequestMapping(value="/alipay/callback/return",method = RequestMethod.GET)
    public String checkBackReturn(){
        String url = "www.baidu.com";
       // return "redirect:"+AlipayConfig.return_order_url;
        return "redirect:"+url;
    }
    //异步回调
    @RequestMapping(value = "/alipay/callback/notify",method = RequestMethod.POST)
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String,String> paramsMap,HttpServletRequest request) throws AlipayApiException {
        // 拿公用key+数据验证
        String sign = request.getParameter("sign");
        boolean flag = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, "utf-8",AlipayConfig.sign_type);
        if (!flag){
            return "fial";
        }
        // 判断结束
        String trade_status = paramsMap.get("trade_status");
        if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
            // 查单据是否处理
            String out_trade_no = paramsMap.get("out_trade_no");
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOutTradeNo(out_trade_no);
            PaymentInfo paymentInfoHas = paymentService.getpaymentInfo(paymentInfo);

            if (paymentInfoHas.getPaymentStatus()==PaymentStatus.PAID || paymentInfoHas.getPaymentStatus()==PaymentStatus.ClOSED){
                return "fail";
            }else {
                // 修改
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                // 设置状态
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                // 设置创建时间
                paymentInfoUpd.setCallbackTime(new Date());
                // 设置内容
                paymentInfoUpd.setCallbackContent(paramsMap.toString());
                paymentService.updataPaymentInfo(paymentInfoUpd,out_trade_no);
                //修改订单状态
                paymentService.sendPaymentResult(paymentInfoUpd,"success");
                return "success";
            }
        }
        return  "fail";
    }

}
