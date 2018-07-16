package com.atguigu.gmall.cart.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {
/*
* 	先检查该用户的购物车里是否已经有该商品
	如果有商品，只要把对应商品的数量增加上去就可以，同时更新缓存
	如果没有该商品，则把对应商品插入到购物车中，同时插入缓存。
*/
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Reference
    private ManageService manageService;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //查看购物车中是否有该商品
        CartInfo cartInfoQuery = new CartInfo();
        cartInfoQuery.setSkuId(skuId);
        cartInfoQuery.setUserId(userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQuery);
        if(cartInfoExist!=null){
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        }else{
            //没有 商品信息
            //根据skuId查找商品信息
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo =  new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.insertSelective(cartInfo);
            cartInfoExist = cartInfo;
        }
        //将购物车数据放到redis中
        //hset
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis =redisUtil.getJedis();
        String toJSONString = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey,skuId,toJSONString);
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userInfoKey,ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> loadCartCache(String useId) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCarPrice(useId);
        if(cartInfoList!=null&&cartInfoList.size()>0){
            //放入jedis在
            Jedis jedis = redisUtil.getJedis();
            String userCartKey = CartConst.USER_KEY_PREFIX+useId+CartConst.USER_CART_KEY_SUFFIX;
            Map<String,String> map = new HashMap<>(cartInfoList.size());
            for (CartInfo cartInfo : cartInfoList) {
                String cartJson = JSON.toJSONString(cartInfo);
                map.put(cartInfo.getSkuId(),cartJson);
            }
            jedis.hmset(userCartKey,map);
            jedis.close();
        }
        return cartInfoList;
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        //看缓存，数据库，缓存key
        String  uesrCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        List<String> cartJsons = jedis.hvals(uesrCartKey);
        //循环，准备一个新的集合
        if(cartJsons!=null&&"".equals(cartJsons)){
            List<CartInfo> cartInfoList = new ArrayList<>();
            for (String cartJson : cartJsons) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            //走数据库要验证价格,skuInfo中的price  cartInfo中的price,将数据库的价格放入缓存
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    @Override
    public List<CartInfo> mergerToCartList(List<CartInfo> cartListFromCookie, String userId) {
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCarPrice(userId);
        for (CartInfo cartInfoCK : cartListFromCookie) {
            //有没有相同的
            boolean  isMatch = false;
            for (CartInfo infoDB : cartInfoListDB) {
                //skuId相同，是同一个商品
               if(cartInfoCK.getSkuId().equals(infoDB.getSkuId())){
                   infoDB.setSkuNum(infoDB.getSkuNum()+cartInfoCK.getSkuNum());
                   cartInfoMapper.updateByPrimaryKeySelective(infoDB);
                   isMatch = true;
               }
            }
            if(!isMatch){
                //userId赋值
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        //先查数据库，再放入缓存
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }
}
