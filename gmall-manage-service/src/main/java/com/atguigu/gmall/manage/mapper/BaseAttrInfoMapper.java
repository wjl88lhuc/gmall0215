package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {


    /**
     * 根据 catalog3Id 查询平台属性集合
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoListByCatalog3(String catalog3Id);

    /**
     * 平台属性值id查询数据
     * @param valueIds
     * @return
     */
    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("valueIds") List valueIds);
//    List<BaseAttrInfo> selectBaseAttrInfoList(List valueIds);
}
