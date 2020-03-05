package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderDetail implements Serializable {
    @Id
    @Column
    private String id;
    @Column
    private String orderId;
    @Column
    private String skuId;
    @Column
    private String skuName;
    @Column
    private String imgUrl;
    @Column
    private BigDecimal orderPrice;
    @Column
    private Integer skuNum;

    /**
     * hasStock是一个非持久化属性，用户传递【是否还有库存】的标志。
     * 如果商品在库存中有足够数据，suceess = “1”，fail=“0”
     */
    @Transient
    private String hasStock;

}
