package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuLsInfo implements Serializable {

    //不加注解是因为这个实体类在数据库中没u对应的表
    String id;

    BigDecimal price;

    String skuName;

    String catalog3Id;

    String skuDefaultImg;

    //自定义字段保存热度
    Long hotScore=0L;

    List<SkuLsAttrValue> skuAttrValueList;

}
