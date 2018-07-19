package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    /*
    * 	先检查该用户的购物车里是否已经有该商品
	如果有商品，只要把对应商品的数量增加上去就可以，同时更新缓存
	如果没有该商品，则把对应商品插入到购物车中，同时插入缓存。
1、	别忘记写mapper
*/
    // 添加购物车
    void  addToCart(String skuId,String userId,Integer skuNum);
    // 根据用户id查询购物车
    List<CartInfo> getCartList(String userId);
    // 合并购物车
    List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);

    void checkCart(String skuId, String isChecked, String userId);

    List<CartInfo> getCartCheckedList(String userId);
}
