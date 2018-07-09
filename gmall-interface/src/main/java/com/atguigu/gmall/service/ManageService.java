package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService{
    public List<BaseCatalog1> getCatalog1();
    public List<BaseCatalog2> getCatalog2(String catalog1Id);
    public List<BaseCatalog3> getCatalog3(String catalog2Id);
    public List<BaseAttrInfo> getBaseAttrInfo(String catalog3Id);
    public List<BaseAttrValue> getAttrValueList(String attrId);
    public BaseAttrInfo getAttrInfo(String attrId);
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    List<BaseSaleAttr> baseSaleAttrList();

    void saveSpuInfo(SpuInfo spuInfo);

    List<BaseAttrInfo> attrInfoList(String catalog3Id);

    List<SpuImage> spuImageList(String spuId);

    List<SpuSaleAttr> spuSaleAttrList(String spuId);

    void saveSku(SkuInfo skuInfo);

    List<SkuInfo> skuInfoListBySpu(String spuId);
}
