package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {
    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;


    //查看详情页的时候用到,该控制器需要登录
    //@LoginRequire(autoRedirect = true)
    @RequestMapping(value = "/{skuId}.html")
    public String item(@PathVariable("skuId") String skuId, Model model) {
        System.out.println(skuId);
        //查询sKuInfo
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        model.addAttribute("skuInfo", skuInfo);
        //查询销售属性和属性值
        List<SpuSaleAttr> saleAttrList = manageService.selectSpuSaleAttrListCkeckBySku(skuInfo);
        model.addAttribute("saleAttrList",saleAttrList);


       List<SkuSaleAttrValue> skuSaleAttrValueListBySpu =
               manageService.selectSkuSaleAttrValue(skuInfo.getSpuId());
        // "1|20" : "17"  "2|10" : "17" valueIdsKey="1|20"  map.put(valueIdsKey,skuId);
        // 先声明一个字符串
        String valueIdsKey = "";
        // 需要定一个map集合
        HashMap<String, String> map = new HashMap<>();
        // 循环拼接
        for (int i = 0; i <skuSaleAttrValueListBySpu.size() ; i++) {
            // 取得第一个值
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            // 什么时候加|
            if (valueIdsKey.length()>0){
                valueIdsKey+="|";
            }
            valueIdsKey+=skuSaleAttrValue.getSaleAttrValueId();

            // 什么时候停止拼接
            if ((i+1)==skuSaleAttrValueListBySpu.size()||
                    !skuSaleAttrValue.getSkuId().
                            equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())){
                map.put(valueIdsKey,skuSaleAttrValue.getSkuId());
                valueIdsKey="";
            }
        }
        // 将map 转换成json字符串
        String valuesSkuJson = JSON.toJSONString(map);

        System.out.println("valueJson:="+valuesSkuJson);

        model.addAttribute("valuesSkuJson",valuesSkuJson);
        listService.incrHotScore(skuId);
        return "item";
    }
}
