package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

@Data
public class SpuImage implements Serializable {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  //获取主键自增
    private String id;
    @Column
    private String spuId;
    @Column
    private String imgName;
    @Column
    private String imgUrl;

}
