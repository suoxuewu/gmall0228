package com.atguigu.gmall.cart.controler;
import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {
       @Reference
        private CartService cartService;
       @Reference
        ManageService manageService;
       @Reference
        CartCookieHandler cartCookieHandler;

        @RequestMapping(value = "cartList")
        @LoginRequire(autoRedirect = false)
        public String cartList(HttpServletRequest request,HttpServletResponse response,Model model){
            //判断是否登录
            String userId = (String) request.getAttribute("userId");
            //获取所有的cartInfo,从cookie中查找
            List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
            List<CartInfo> cartList = null;
            if(userId!=null){
                if(cartListFromCookie!=null&&cartListFromCookie.size()>0){
                    //合并购物车
                    cartList = cartService.mergerToCartList(cartListFromCookie,userId);
                    //删除cookie
                    cartCookieHandler.deleteCartCookie(request,response);
                }
                model.addAttribute("cartList",cartList);
            }else {
                //从cookie中获取
                List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
                model.addAttribute("cartList",cookieHandlerCartList);
            }
            return "cartList";
        }


        @RequestMapping(value="addToCart",method = RequestMethod.POST)
        public String addToCart(HttpServletRequest request, HttpServletResponse response, Model model){
            String skuNum = request.getParameter("skuNum");
            String skuId = request.getParameter("skuId");
            String userId = (String) request.getAttribute("userId");
            //已经登录
            if(userId!=null){
                cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
            }else {
                //做cookie
                cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
            }
            //获取skuinfo信息，根据前台需要
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            model.addAttribute("skuInfo",skuInfo);
            model.addAttribute("skuNum",skuNum);
            return "success";
        }
}
