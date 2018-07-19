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

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtil redisUtil;
    // 需要调用getSkuInfo(userId)

    @Reference
    private ManageService manageService;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        // 查看购物车CartInfo中是否有该商品
        CartInfo cartInfoQuery = new CartInfo();
        cartInfoQuery.setSkuId(skuId);
        cartInfoQuery.setUserId(userId);

        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfoQuery);
        if (cartInfoExist !=null){
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            // 更改也需要变动缓存
        }else{
            // 没有创建购物车
            // 根据skuId 查找商品信息
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuName(skuInfo.getSkuName());
            // 实施价格
            cartInfo.setSkuPrice(skuInfo.getPrice());
            // 添加购物车时候的价格
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            // 新添加的购物车插入的时候，也需要插入缓存
            cartInfoMapper.insertSelective(cartInfo);
            // 将插入的信息也给已经存在的购物车对象
            cartInfoExist=cartInfo;

        }
        // 想办法将购物车数据放到redis中
        // hset(key,field,value) :key = （user:userId:cart）
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 获取redis对象
        Jedis jedis = redisUtil.getJedis();
        // 保存的数据
        String cartJson  = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey,skuId,cartJson );
        // 细节的地方！user:userId+:info
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        // 看缓存，数据库！   缓存key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 创建redis对象 hset(key,field,value); hget(key,field) hget(userCartKey , skuId);
        Jedis jedis = redisUtil.getJedis();
        // redis=hash --- java=list;
        List<String> cartJsons  = jedis.hvals(userCartKey);
        // 循环
        // 准备一个新的集合：
        if (cartJsons!=null && !"".equals(cartJsons)){
            List<CartInfo> cartInfoList = new ArrayList<>();
            for (String cartJson : cartJsons) {
                // 将对象转换成cartInfo对象
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            // hash 有顺序么？ 根据id进行排序 time 外部比较器
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            // 走数据库，验价过程！ sku_Info 中的price ，car_Info cartprice ,将数据库中的数据放入缓存
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return  cartInfoList;

        }

    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId) {
        // 循环判断
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCarPrice(userId);
        for (CartInfo cartInfoCK : cartListFromCookie) {
            // 有相同的，没有相同[insert]
            boolean isMatch = false;
            for (CartInfo infoDB : cartInfoListDB) {
                // 如果skuId 相同，则说明是同一个商品，则数据要增加
                if (cartInfoCK.getSkuId().equals(infoDB.getSkuId())){
                    infoDB.setSkuNum(cartInfoCK.getSkuNum()+infoDB.getSkuNum());
                    cartInfoMapper.updateByPrimaryKeySelective(infoDB);
                    isMatch=true;
                }
            }
            // 插入信息
            if (!isMatch){
                // userId 赋值
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        // loadCartCache ： 根据userId 先查数据库，在放缓存
        List<CartInfo> cartInfoList = loadCartCache(userId);
        for (CartInfo cartInfo : cartInfoList) {
            for (CartInfo info : cartListFromCookie) {
                if(cartInfo.getSkuId().equals(info.getSkuId())){
                    if("1".equals(info.getIsChecked())){
                        cartInfo.setIsChecked("1");
                        checkCart(cartInfo.getSkuId(),info.getIsChecked(),userId);
                    }
                }
            }
        }
        return cartInfoList;
    }

    @Override
    /*把对应skuId的购物车的信息从redis中取出来，反序列化，修改isChecked标志。
再保存回redis中。
同时保存另一个redis的key 专门用来存储用户选中的商品，方便结算页面使用。
*/
    public void checkCart(String skuId, String isChecked, String userId) {
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        String cartJson = jedis.hget(userCartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));

        String userCkeckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if("1".equals(isChecked)){
            jedis.hset(userCkeckedKey,skuId,JSON.toJSONString(cartInfo));
        }else{
            jedis.del(userCkeckedKey,skuId);
        }
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        String userCkeckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        List<String> cartCheckedList  = jedis.hvals(userCkeckedKey);
        List<CartInfo> newCartList = new ArrayList<>();
        for (String cartJson  : cartCheckedList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            newCartList.add(cartInfo);
        }
        return newCartList;
    }

    public List<CartInfo> loadCartCache(String userId) {
        // 在mapper中写个方法，
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCarPrice(userId);
        //  判断集合
        if (cartInfoList!=null && cartInfoList.size()>0){
            // 准备放入redis
            Jedis jedis = redisUtil.getJedis();
            // 对数据进行转换 hset(key,field,value);
            String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
            // field，value === 正好对应上我们的map.put(field,value) jedis.hmset(userCartKey,map);
            Map<String,String> map = new HashMap<>(cartInfoList.size());
            for (CartInfo cartInfo : cartInfoList) {
                // 将cartInfo 转换成对象
                String cartJson  = JSON.toJSONString(cartInfo);
                map.put(cartInfo.getSkuId(),cartJson);
            }
            // 往redis 中添加数据
            jedis.hmset(userCartKey,map);
            jedis.close();
        }
        return cartInfoList;
    }
}
