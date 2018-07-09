package com.atguigu.gmall.usermanage.serviceImpl;


import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
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

}
