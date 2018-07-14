package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SkuController {
    @Reference
    private ManageService manageService;
    //rid({url:"skuInfoListBySpu?spuId="+spuId});
    @RequestMapping("skuInfoListBySpu")
    @ResponseBody
    public List<SkuInfo> skuInfoListBySpu(String spuId) {
        return  manageService.skuInfoListBySpu(spuId);
    }
//从spu中选择sku
    //attrInfoList?catalog3Id="+catalog3Id
    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> spuList(String catalog3Id) {
        return  manageService.attrInfoList(catalog3Id);
    }
    //url:'spuImageList?spuId='+spuId}
    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(String spuId) {
        List<SpuImage> spuImageList =  manageService.spuImageList(spuId);
        return spuImageList;
    }
    //$.get("spuSaleAttrList?spuId="+spuId,function(data){
    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        List<SpuSaleAttr> spuSaleAttrList =  manageService.spuSaleAttrList(spuId);
        return spuSaleAttrList;
    }
    //$.post("/saveSku",skuInfo,function (data) {
    @RequestMapping(value="saveSku",method = RequestMethod.POST)
    @ResponseBody
    public String saveSku(SkuInfo skuInfo) {
        manageService.saveSku(skuInfo);
        String skuId = skuInfo.getId();
        return "redirect:/onSale?skuId="+skuId;
    }
}
