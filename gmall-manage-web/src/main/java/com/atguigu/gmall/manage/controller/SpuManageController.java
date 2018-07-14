package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SpuManageController {
    @Reference
    private ManageService manageService;


    @RequestMapping("spuListPage")
    public String spuListPage() {
        return "spuListPage";
    }

    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> spuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> spuInfoList =  manageService.getSpuInfoList(spuInfo);
        return spuInfoList;
    }
    //保存商品
    @RequestMapping(value = "saveSpuInfo",method = RequestMethod.POST)
    @ResponseBody
    public void saveSpuInfo(SpuInfo spuInfo) {
        manageService.saveSpuInfo(spuInfo);
//        spuInfo.get
//        return "redirect:/onSale?skuId="
    }
    //return new ModelAndView
    // ("redirect:/stock/downLoadFile.htmls?fileName="+newFileName+"&newFilePath="+newFilePath);
}
