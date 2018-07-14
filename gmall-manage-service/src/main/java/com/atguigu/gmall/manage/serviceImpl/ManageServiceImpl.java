package com.atguigu.gmall.manage.serviceImpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService{
    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    SpuImageMapper spuImageMapper;
    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    SpuInfoMapper spuInfoMapper;
    @Autowired
    BaseCataLog1Mapper baseCataLog1Mapper;
    @Autowired
    BaseCataLog2Mapper baseCataLog2Mapper;
    @Autowired
    BaseCataLog3Mapper baseCataLog3Mapper;
    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuImageMapper skuImageMapper;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCataLog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCataLog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCataLog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getBaseAttrInfo(String catalog3Id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        return baseAttrInfoMapper.select(baseAttrInfo);
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        return null;
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        //查询对应的属性值
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        if (baseAttrInfo.getId()!=null && baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        }else {
            if(baseAttrInfo.getId().length()==0){
                baseAttrInfo.setId(null);
            }
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);

        if (baseAttrInfo.getAttrValueList()!=null&&baseAttrInfo.getAttrValueList().size()>0){
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                if (attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        List<SpuInfo> spuInfoList = spuInfoMapper.select(spuInfo);
        return spuInfoList;
    }

    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        if(spuInfo.getId()!=null&&spuInfo.getId().length()>0){
            spuInfoMapper.updateByPrimaryKey(spuInfo);
        }else {
            if(spuInfo.getId()!=null&&spuInfo.getId().length()==0){
                spuInfo.setId(null);
            }
            spuInfoMapper.insertSelective(spuInfo);
        }
        //先删除再插入
        SpuImage spuImage = new SpuImage();
        spuImage.setId(spuInfo.getId());
        spuImageMapper.deleteByPrimaryKey(spuImage);
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        for (SpuImage image : spuImageList) {
            if(image.getId()!=null&&image.getId().length()==0){
                image.setId(null);
            }
            image.setSpuId(spuInfo.getId());
            spuImageMapper.insertSelective(image);
        }

        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);
        SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuInfo.getId());
        spuSaleAttrValueMapper.delete(spuSaleAttrValue);

        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        for (SpuSaleAttr saleAttr : spuSaleAttrList) {
            if(saleAttr.getId()!=null && saleAttr.getId().length()==0){
                saleAttr.setId(null);
            }
            saleAttr.setSpuId(spuInfo.getId());
            spuSaleAttrMapper.insertSelective(saleAttr);

            //插入属性值
            List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
            for (SpuSaleAttrValue saleAttrValue : spuSaleAttrValueList) {
                if(saleAttrValue.getId()!=null && saleAttrValue.getId().length()==0){
                    saleAttrValue.setId(null);
                }
                saleAttrValue.setSpuId(spuInfo.getId());
                spuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrInfo> attrInfoList(String catalog3Id) {
        return  baseAttrInfoMapper.selectAttrInfoList(Long.parseLong(catalog3Id));
    }

    @Override
    public List<SpuImage> spuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        List<SpuImage> spuImageList = spuImageMapper.select(spuImage);
        return spuImageList;
    }

    @Override
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(Long.parseLong(spuId));
        return spuSaleAttrList;
    }

    @Override
    public void saveSku(SkuInfo skuInfo) {
        if (skuInfo.getId()==null || skuInfo.getId().length()==0){
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        } else {
            skuInfoMapper.updateByPrimaryKey(skuInfo);
        }

        // 先删除，再添加
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        // SkuId = SkuInfo.id
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue attrValue : skuAttrValueList) {
            // 坑！
            attrValue.setSkuId(skuInfo.getId());
            if (attrValue.getId()!=null&& attrValue.getId().length()==0){
                attrValue.setId(null);
            }
            skuAttrValueMapper.insertSelective(attrValue);
        }
        // 属性值添加
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
            saleAttrValue.setSkuId(skuInfo.getId());
            if (saleAttrValue.getId()!=null && saleAttrValue.getId().length()==0){
                saleAttrValue.setSkuId(null);
            }
            skuSaleAttrValueMapper.insertSelective(saleAttrValue);
        }
        // 图片添加
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage image : skuImageList) {
            image.setSkuId(skuInfo.getId());
            if (image.getId()!=null && image.getId().length()==0){
                image.setId(null);
            }
            skuImageMapper.insertSelective(image);
        }

    }

    @Override
    public List<SkuInfo> skuInfoListBySpu(String spuId) {
        List<SkuInfo> skuInfoList = skuInfoMapper.selectSkuInfoListBySpu(Long.parseLong(spuId));
        return skuInfoList;
    }



    private SkuInfo getSkuInfoDB() {
        return null;
    }

    //提取代码
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        SkuInfo skuInfo = null;
        String skuInfoKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
        if(jedis.exists(skuInfoKey)){
            String skuInfoJson = jedis.get(skuInfoKey);
            if(skuInfoJson!=null&&skuInfoJson.length()!=0){
                skuInfo = JSON.parseObject(skuInfoJson,SkuInfo.class);
            }
        }else {
            skuInfo = getSkuInfoDB(skuId);
            String jsonString = JSON.toJSONString(skuInfo);
            jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,jsonString);
        }
        jedis.close();
        return skuInfo;
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);

        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList =   skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);

        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuId);
        List<SkuSaleAttrValue> skuSaleAttrValueList =
                skuSaleAttrValueMapper.select(skuSaleAttrValue);
        skuInfo.setSkuSaleAttrValueList(skuSaleAttrValueList);

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> selectSpuSaleAttrListCkeckBySku(SkuInfo skuInfo) {
        List<SpuSaleAttr> spuSaleAttrList =
        spuSaleAttrMapper.selectSpuSaleAttrListCkeckBySku
         (Long.parseLong(skuInfo.getId()), Long.parseLong(skuInfo.getSpuId()));
        return spuSaleAttrList;
    }

    @Override
    public List<SkuSaleAttrValue> selectSkuSaleAttrValue(String skuId) {
        List<SkuSaleAttrValue> skuSaleAttrValueList =
                skuSaleAttrValueMapper.selectSkuSaleAttrValue(skuId);
        return skuSaleAttrValueList;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
       // 将集合id，进行分割 ["13","80","14","20","22","81","82","83","85","87"]
        String valueId = StringUtils.join(attrValueIdList, ",");
        System.out.println("分割后的自符串valueId="+valueId);
        List<BaseAttrInfo> baseAttrInfos = baseAttrInfoMapper.selectAttrInfoListByIds(valueId);
        return baseAttrInfos;
    }
}
