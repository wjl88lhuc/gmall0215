package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;

import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("/list.html")
    public String getList(SkuLsParams skuLsParams,  Model model){

        // 设置每页显示的数据条数
        skuLsParams.setPageSize(2);

        SkuLsResult skuLsResult = listService.search(skuLsParams);
        // 从结果中取出平台属性值列表
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);

        // 已选的属性值列表\
        String urlParam = makeUrlParam(skuLsParams);

        //定义一个面包屑集合
        List<BaseAttrValue> baseAttrValueList = new ArrayList<>();

        // itco
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo =  iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                    for (String valueId : skuLsParams.getValueId()) {
                        //选中的属性值 和 查询结果的属性值
                        if(valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                            //面包屑的组成
//                            baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName();
                            BaseAttrValue baseAttrValueed = new BaseAttrValue();
                            baseAttrValueed.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());

                            //将用户点击的平台属性值id传递到 makeUrlParam方法 中，重新制作返回的url参数
                            String newUrlParam = makeUrlParam(skuLsParams,valueId);
                            baseAttrValueed.setUrlParam(newUrlParam);
                            baseAttrValueList.add(baseAttrValueed);
                        }
                    }
                }
            }
        }

        model.addAttribute("baseAttrValueList",baseAttrValueList);

        model.addAttribute("urlParam",urlParam);

        // 保存分页的数据：
        model.addAttribute("pageNo",skuLsParams.getPageNo());
        model.addAttribute("totalPages",skuLsResult.getTotalPages());

        //保存一个检索的关键字
        model.addAttribute("keyword",skuLsParams.getKeyword());
        model.addAttribute("baseAttrInfoList",attrList);
        // 获取sku属性值列表
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        model.addAttribute("skuLsInfoList",skuLsInfoList);
        //return JSON.toJSONString(search);
        return "list";
    }

    /**
     * 判断url后面有哪些参数
     * @param skuLsParam
     * @Param extandParams 点击面包屑是获取的是平台属性值id ,并且只有一个值，因为在前端页面点击的时候只传一个值
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParam,String ... extandParams) {
        String urlParam="";
        if(skuLsParam.getKeyword()!=null){
            urlParam+="keyword="+skuLsParam.getKeyword();
        }
        if (skuLsParam.getCatalog3Id()!=null){
            if (urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParam.getCatalog3Id();
        }
        String[] valueIds = skuLsParam.getValueId();
        // 构造属性参数
        if (valueIds!=null && valueIds.length>0){
            for (String valueId : valueIds){

                if (extandParams != null && extandParams.length > 0){
                    //获取点击面包屑时获取的属性值id
                    String extendValuId = extandParams[0];
                    if (extendValuId.equals(valueId)){
                        // break
                        continue;
                    }
                }
                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }
        return  urlParam;

    }

}
