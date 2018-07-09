package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr>{

    public List<SpuSaleAttr> selectSpuSaleAttrList(Long spuId);


    public List<SpuSaleAttr> selectSpuSaleAttrListCkeckBySku(long skuId,long spuId);
}
