package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1() {
        return manageService.getBaseCatalog1();
    }

    /**
     * http://localhost:8082/getCatalog2?catalog1Id=6
     *
     * @param catalog1Id
     * @return
     */
    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        return manageService.getBaseCatalog2(catalog1Id);
    }

    /**
     * http://localhost:8082/getCatalog3?catalog2Id=6
     *
     * @param catalog2Id
     * @return
     */
    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        return manageService.getBaseCatalog3(catalog2Id);
    }

    //http://localhost:8082/attrInfoList?catalog3Id=228
    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> getAttrInfoList(String catalog3Id) {
        return manageService.getAttrInfoList(catalog3Id);
    }


    //将前台传递过来的json数据转换成为对象： @RequestBody
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
    }

    //http://localhost:8082/getAttrValueList?attrId=23
//    @RequestMapping("getAttrValueList")
//    public List<BaseAttrValue> getAttrValueList(String attrId){
//        return manageService.getAttrValueList(attrId);
//    }

    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        //这样做的原因比较符合实际，因为先有平台属性，采用属性值
        //先通过 attrId 查询平台属性 select  * from baseAttrInfo where id = attrId
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        return baseAttrInfo.getAttrValueList();
    }

    /**
     * http://localhost:8082/baseSaleAttrList
     * @return
     */
    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }




}
