package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ListController {
    @Reference
    ListService listService;
    /**这里用4或者5
     * http://localhost:8086/list?keyword=小米&catalog3Id=61&valueId=1&pageNo=1&pageSize=1
     * */
    @RequestMapping(value = "list",method = RequestMethod.GET)
    @ResponseBody
    public String getList(SkuLsParams skuLsParams){
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        String search = JSON.toJSONString(skuLsResult);
        System.out.println(search);
        return search;
    }
}
