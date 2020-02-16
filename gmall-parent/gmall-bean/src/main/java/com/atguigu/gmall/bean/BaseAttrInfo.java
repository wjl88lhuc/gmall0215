package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
public class BaseAttrInfo implements Serializable {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)  //获取主键自增
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    //@Transient 表示不需要序列化的属性，也就是说这个属性不是表中的字段，而只是业务需求恰巧在这里额外添加的
    @Transient
    private List<BaseAttrValue> attrValueList;



}
