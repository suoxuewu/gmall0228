package com.atguigu.gmall.cart.controler;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.CookieUtil;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {
    private String cookieCartName = "CART";
    private int COOKIE_CART_MAXAGE = 7*24*3600;

    @Reference
    private ManageService manageService;

    public void addToCart(HttpServletRequest request, HttpServletResponse response,String skuId, String userId, Integer skuNum){
        String cartJson = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = new ArrayList<>();
        boolean ifExist = false;
        if(cartJson!=null){
            cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
            for (CartInfo cartInfo : cartInfoList) {
                if(cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    ifExist=true;
                }
            }
        }
        if(!ifExist){
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            //将每个商品都放入集合中
            cartInfoList.add(cartInfo);
        }
        //将集合转化为字符串
        String newCartJson = JSON.toJSONString(cartInfoList);
        CookieUtil.setCookie(request,response,cookieCartName,newCartJson,COOKIE_CART_MAXAGE,true);
    }

    public List<CartInfo> getCartList(HttpServletRequest request) {
        //调用工具类
        String cartJson = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
        return cartInfoList;
    }

    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }
}
