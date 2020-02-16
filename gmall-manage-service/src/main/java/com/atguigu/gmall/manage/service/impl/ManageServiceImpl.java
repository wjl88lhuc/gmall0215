package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Override
    public List<BaseCatalog1> getBaseCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getBaseCatalog2(String catalog1) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getBaseCatalog3(String catalog2) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(String catalog3) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3);
        return baseAttrInfoMapper.select(baseAttrInfo);
    }

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        String id = baseAttrInfo.getId();
        if (id == null){
            System.out.println("****单独id********");
        }else{
            System.out.println("****步步步步********");
        }

        //修改操作
        if (baseAttrInfo.getId() != null){
            System.out.println("*************大家后才是真的号************");
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else{
            //保存数据 baseAttrInfo
            System.out.println("********加入了else语句********");
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        //保存数据 baseAttrValue: 先清空，后再插入数据即可
        //根据attrid进行清空: delete from baseAttrValue where attrId = baseAttrInfo.getId();
        BaseAttrValue baseAttrValuedel = new BaseAttrValue();
        baseAttrValuedel.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValuedel);

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList != null && attrValueList.size() >0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //baseAttrInfo.getId()的前提是baseAttrInfo的对象能够获取得到主键的自增的值
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return baseAttrValueMapper.select(baseAttrValue);
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        //需要将平台属性值集合放入到平台属性中
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        baseAttrInfo.setAttrValueList(baseAttrValueMapper.select(baseAttrValue));

        return baseAttrInfo;

    }
}
