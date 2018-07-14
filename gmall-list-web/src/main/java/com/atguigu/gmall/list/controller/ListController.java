package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    ListService listService;
    @Reference
    ManageService manageService;

    @RequestMapping(value = "ee")
    public String saveAttrInfo() {
        return "forward:www.baidu.com";
   }
    /**
     * 这里用4或者5
     * http://localhost:8086/list?keyword=小米&catalog3Id=61&valueId=1&pageNo=1&pageSize=1
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET)
    public String getList(SkuLsParams skuLsParams, Model model) {

//        if(skuLsParams.getValueId()==null){
//            return "redirect:/ee";
//        }
        skuLsParams.setPageSize(2);
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        String search = JSON.toJSONString(skuLsResult);
        System.out.println(search);
        model.addAttribute("skuLsInfoList", skuLsResult.getSkuLsInfoList());
        //获取平台属性值列表
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
        model.addAttribute("attrList",attrList);


        //找出那些属性被选中了
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        //做URL,参数skuLsParam
        String makeUrl = makeUrlParam(skuLsParams);
        //去电skulsparam中与makeurl相同部分条件
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            //获得每一个baseAttrInfo对象
            BaseAttrInfo baseAttrInfo =  iterator.next();
            //拿到属性值
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if(baseAttrValue.getId()!=null&&baseAttrValue.getId().length()>0){
                    if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                        for (String valueId : skuLsParams.getValueId()) {
                            if(valueId.equals(baseAttrValue.getId())){
                                //去重
                                iterator.remove();
                                //穿件一个被选中属性值得对象
                                //构造面包屑列表
                                BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                                //屏幕尺寸:5.1-5.5英寸
                                baseAttrValueSelected.setValueName
                                (baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                                //添加之前去重
                                String urlParam = makeUrlParam(skuLsParams, valueId);
                                baseAttrValueSelected.setUrlParam(urlParam);
                                //添加到被选中的集合中
                                baseAttrValueArrayList.add(baseAttrValueSelected);
                            }
                        }
                    }
                }
            }
        }
        //分页
        int totalPages = (int) ((skuLsResult.getTotal()+skuLsParams.getPageSize()-1)
                /skuLsParams.getPageSize());
        model.addAttribute("totalPages",totalPages);
        //当前页码
        model.addAttribute("pageNo",skuLsParams.getPageNo());
        //保存面包屑的清单
        model.addAttribute("urlParam",makeUrl);
        //将备选中的属性值得集合保存起来
        model.addAttribute("baseAttrValuesList",baseAttrValueArrayList);
        //全局检索的值
        model.addAttribute("keyword",skuLsParams.getKeyword());
        return "list";

        //将url保存
    }

    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String makeUrl = "";
        if(skuLsParams.getKeyword()!=null&&skuLsParams.getKeyword().length()>0){
            makeUrl+="keyword="+skuLsParams.getKeyword();
        }
        //三级分类id
        if(skuLsParams.getCatalog3Id()!=null&&skuLsParams.getCatalog3Id().length()>0){
            if(makeUrl.length()>0){
                makeUrl+="&";
              }
              makeUrl+="catalog3Id="+skuLsParams.getCatalog3Id();
          }
        //属性Id
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            for (int i = 0; i <skuLsParams.getValueId().length ; i++) {
                String valueId = skuLsParams.getValueId()[i];
                //传递进来的值，和valueId做比较，如果一样就不拼接
                if(excludeValueIds !=null&&excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(valueId)){
                        continue;
                    }
                }
                if(makeUrl.length()>0){
                    makeUrl+="&";
                }
                makeUrl+="valueId="+valueId;
            }
          }
          return makeUrl;
    }
}
