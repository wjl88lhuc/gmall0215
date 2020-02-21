package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    /**
     * 根据spuId查询spu销售属性集合
     * 涉及到多表查询需要使用到mapper.xml文件来自定义复杂的sql语句
     * SpuSaleAttrMapper.xml
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    /**
     *
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@Param("skuId") String skuId,@Param("spuId") String spuId);
}
