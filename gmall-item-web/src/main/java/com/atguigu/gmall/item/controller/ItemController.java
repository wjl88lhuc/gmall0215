package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuImage;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
    //用户在访问商品详情的时候原本是不需要登陆的，这里是为了测试，所以设置成为了需要先登陆
    @LoginRequire(autoRedirect = true)
    public String item(@PathVariable String skuId, HttpServletRequest request){
        //根据skuId 获取数据
        SkuInfo skuInfo = manageService.getSkuInfoBySkuId(skuId);

        //查询销售属性，销售属性值集合
        List<SpuSaleAttr> spuSaleAttrList = manageService.selectSpuSaleAttrListCheckBySku(skuInfo);

        //获取销售属性值集合
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        String valueIdsKey = "";
        Map<String,Object> valuesSkuMap = new HashMap<>();

        System.out.println("************到这里了****************");
        //遍历 skuSaleAttrValueList 拼接
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            if(valueIdsKey.length()!=0){
                valueIdsKey= valueIdsKey+"|";
            }
            valueIdsKey=valueIdsKey+skuSaleAttrValue.getSaleAttrValueId();

            if((i+1)== skuSaleAttrValueListBySpu.size()||!skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())  ){
                valuesSkuMap.put(valueIdsKey,skuSaleAttrValue.getSkuId());
                valueIdsKey="";
            }

        }

        //把map变成json串
        String valuesSkuJson = JSON.toJSONString(valuesSkuMap);

        request.setAttribute("valuesSkuJson",valuesSkuJson);


        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        request.setAttribute("skuInfo",skuInfo);
        System.out.println("**********方法块结束了***********");
        listService.incrHotScore(skuId);
        return "item";
    }
}
