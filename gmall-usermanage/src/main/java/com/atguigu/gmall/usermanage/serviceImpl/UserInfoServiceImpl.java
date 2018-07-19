package com.atguigu.gmall.usermanage.serviceImpl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix = "user:";
    public String userInfoKey_suffix = ":info";
    public int userInfoKey_timeOut = 60*60;



    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> findLikeUserInfo() {
        // 创建一个Example 对象
      Example example  =  new Example(UserInfo.class);
      example.createCriteria().andLike("loginName","%a%");
      List<UserInfo> userInfos = userInfoMapper.selectByExample(example);
      return  userInfos;
    }

    @Override
    public void addUserInfo(UserInfo userInfo) {
        userInfoMapper.insertSelective(userInfo);
    }

    @Override
    public void upd(UserInfo userInfo) {
        userInfoMapper.updateByPrimaryKey(userInfo);
    }

    @Override
    public void upd1(UserInfo userInfo) {
       Example example = new Example(UserInfo.class);
       example.createCriteria().andEqualTo("loginName",userInfo.getLoginName());
       userInfoMapper.updateByExampleSelective(userInfo,example);
    }

    @Override
    public void del(UserInfo userInfo) {
        userInfoMapper.deleteByPrimaryKey(userInfo);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(password);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if(info!=null){
            //将用户储存到Redis中
            Jedis jedis = redisUtil.getJedis();
            String setex = jedis.setex(userKey_prefix + info.getId() + userInfoKey_suffix, userInfoKey_timeOut,
                    JSON.toJSONString(info));
            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String key = userKey_prefix+userId+userInfoKey_suffix;
        String userJson = jedis.get(key);
        //延长时间
        jedis.expire(key,userInfoKey_timeOut);
        if(userJson!=null){
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            return userInfo;
        }
        return null;
    }
}
