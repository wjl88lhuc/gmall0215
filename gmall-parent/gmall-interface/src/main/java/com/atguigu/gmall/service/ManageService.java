package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService {

    /**
     * 获取所有的一级分类
     * @return
     */
    List<BaseCatalog1>  getBaseCatalog1();

    /**
     * 根据baseCatalog1的id获取对应的catalog2的所有数据
     *
     * @param catalog1
     * @return
     */
    List<BaseCatalog2> getBaseCatalog2(String catalog1);

    /**
     * 根据baseCatalog2的id(二级分类)获取对应的catalog3（三级分类）的所有数据
     * @param catalog2
     * @return
     */
    List<BaseCatalog3> getBaseCatalog3(String catalog2);

    /**
     * 根据三级分类id查询平台属性集合
     * @param catalog3
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(String catalog3);


    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrValueList(String attrId);

    /**
     * 根据平台属性Id 查询平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(String attrId);
}
