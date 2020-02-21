package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SkuAttrValue;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {

    //根据spuId查询 SkuSaleAttrValue
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(@Param("spuId") String spuId);
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}
