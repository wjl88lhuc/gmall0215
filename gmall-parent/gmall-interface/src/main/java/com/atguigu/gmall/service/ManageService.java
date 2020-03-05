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

    /**
     * 根据 spuInfo的catalog3Id属性获取 SpuInfo集合
     * @param spuInfo
     * @return
     */
    List<SpuInfo> getSpuInfo(SpuInfo spuInfo);

    // 查询所有的基本销售属性数据
    List<BaseSaleAttr> getBaseSaleAttrList();

    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * sql: select * from spuImage where spuId = spuImage.getSpuId()
     * @param spuImage
     * @return
     */
    List<SpuImage> getSuImageList(SpuImage spuImage);

    /**
     * 根据spuId获取销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    void saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuInfoBySkuId(String skuId);

    List<SkuImage> getSkuImageBySkuId(String skuId);

    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据spuId查询销售属性值
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     *根据平台属性id查询
     * @param
    attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
