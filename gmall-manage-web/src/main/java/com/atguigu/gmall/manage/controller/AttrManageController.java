package com.atguigu.gmall.manage.controller;
import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Controller
public class AttrManageController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;
    @RequestMapping("attrListPage")
    public String attrListPage() {
        return "attrListPage";
    }



    @RequestMapping(value = "getCatalog1", method = RequestMethod.POST)
    @ResponseBody
    public List<BaseCatalog1> getCatalog1() {
        return manageService.getCatalog1();
    }

    @RequestMapping(value = "getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog1(String catalog1Id) {
        return manageService.getCatalog2(catalog1Id);
    }

    @RequestMapping(value = "getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        return manageService.getCatalog3(catalog2Id);
    }

    @RequestMapping(value = "getAttrList")
    @ResponseBody
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        return manageService.getBaseAttrInfo(catalog3Id);
    }

    @RequestMapping(value = "getAttrValueList")
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrInfo attrInfo = manageService.getAttrInfo(attrId);
        return attrInfo.getAttrValueList();

    }

    @RequestMapping(value = "saveAttrInfo",method = RequestMethod.POST)
    @ResponseBody
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        System.out.println(baseAttrInfo);
        manageService.saveAttrInfo(baseAttrInfo);
    }
    @RequestMapping(value = "onSale",method = RequestMethod.GET)
    @ResponseBody
    public void saveSkuLssInfo(String skuId){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        try {
            BeanUtils.copyProperties(skuLsInfo,skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        //千万不要忘了保存
        listService.saveSkulsInfo(skuLsInfo);
    }
}
