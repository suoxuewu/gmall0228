package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    public List<BaseAttrInfo> selectAttrInfoList(Long catalog3Id);

    List<BaseAttrInfo> selectAttrInfoListByIds(@Param(value = "valueId") String valueId);
}
