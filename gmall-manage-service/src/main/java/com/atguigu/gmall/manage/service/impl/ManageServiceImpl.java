package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;


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
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3);
//        return baseAttrInfoMapper.select(baseAttrInfo);



        return baseAttrInfoMapper.selectAttrInfoListByCatalog3(catalog3);
    }

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //修改操作
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0){
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else{
            //保存数据 baseAttrInfo
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

    @Override
    public List<SpuInfo> getSpuInfo(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        spuInfoMapper.insertSelective(spuInfo);


        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0){
            for (SpuImage spuImage : spuImageList) {
                //设置spuId
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);//插入图片
            }
        }

        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSuImageList(SpuImage spuImage) {
        //sql: select * from spuImage where spuId = spuImage.getSpuId()
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        //设计到两张表
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    //保存skuInfo数据
    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {

        System.out.println("打印传递过来的数据：");
        System.out.println(skuInfo);

        if (skuInfo == null){
            return;
        }
        //skuInfo
        skuInfoMapper.insertSelective(skuInfo);
        List<SkuInfo> skuInfos = skuInfoMapper.selectAll();
        System.out.println("查询skuInfos的所有内容，条数一共是： " + skuInfos.size());

        //spuImage
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuImageList.size() > 0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }

    public RLock getRLock(String lockName){
        Config config = new Config();
        //setAddress("redis://dsjrz81:6379")
        config.useSingleServer().setAddress("redis://"+ host +":"+port);
        RedissonClient redissonClient = Redisson.create(config);
        RLock rLock = null;
        try {
            rLock = redissonClient.getLock(lockName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return rLock;
        }
    }

    @Override
    public SkuInfo getSkuInfoBySkuId(String skuId) {
        RLock skuLock = null;
        //从redis连接池中获取redis连接
        Jedis jedis = redisUtil.getJedisFromPool();
        if (jedis == null){ // 如果redis连接失败，则说明redis缓存宕机了，跳过redis,直接查询数据库，直接返回
            return getSkuInfoDB(skuId);
        }
        //定义key,命名：见名知意:   sku:skuId:info
        String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
        SkuInfo skuInfo  = null;
        try
        {
            //判断缓存中是否有数据，如果有数据则将缓存中的数据直接返回给客户端
            //如果缓存中没有要查询的数据，则从DB数据库中查询数据，然后将数据放入到缓存中并返回给客户端
            if (!jedis.exists(skuKey)){
                skuLock= getRLock("skuLock");
                if (skuLock == null){//如果获取锁失败，则说明redis缓存宕机了，跳过redis,直接查询数据库，直接返回
                    return getSkuInfoDB(skuId);
                }
                skuLock.lock(10, TimeUnit.SECONDS);  // 加锁，如果超过10秒钟，锁就自动失效
                skuInfo = getSkuInfoDB(skuId);  //从数据库中查询
                //将从数据库中新查询得到的数据再放入到redis缓存中,并设置过期时间
                jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
            }else{
                String skuJson = jedis.get(skuKey);// 如果redis缓存中有，就直接返回即可。
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (jedis != null){
                jedis.close();  //关闭redis连接
            }
            if (skuLock != null){
                skuLock.unlock(); // 解锁
            }
            return skuInfo;
        }
    }

    private SkuInfo getSkuInfoJedis(String skuId) {
        Jedis jedis = redisUtil.getJedisFromPool();
        //定义key,命名：见名知意:   sku:skuId:info
        String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
        SkuInfo skuInfo  = null;
        try {
            ///判断缓存中是否有数据，如果有数据则将缓存中的数据直接返回给客户端
            //如果缓存中没有要查询的数据，则从DB数据库中查询数据，然后将数据放入到缓存中并返回给客户端
            String skuJson = jedis.get(skuKey);
            if (skuJson == null || skuJson.length() == 0){
                //试着枷锁
                //定义上锁的key
                String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                String isOk = jedis.set(skuLockKey, "good", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);  //加锁
                if ("OK".equals(isOk)){
                    //枷锁成功
                    skuInfo = getSkuInfoDB(skuId);
                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                    //解锁
                    jedis.del(skuLockKey);
                }else{
                    //等待
                    skuInfo = getSkuInfoBySkuId(skuId);
                }
            }else{
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null){
                jedis.close();// 关闭连接jedis
            }
            if (skuInfo != null){
                return skuInfo;
            }else{
                return getSkuInfoDB(skuId); // 如果redis宕机了，则直接跳过redis,直接从数据库中查询数据即可
            }
        }
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        List<SkuImage> skuImageList = getSkuImageBySkuId(skuId);
        SkuInfo skuInfoResult = skuInfoMapper.selectByPrimaryKey(skuId);
        skuInfoResult.setSkuImageList(skuImageList);
        return skuInfoResult;
    }

    /**
     * 根据skuId查询SkuImage集合
     * @param skuId
     * @return
     */
    @Override
    public List<SkuImage> getSkuImageBySkuId(String skuId) {
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        return skuImageMapper.select(skuImage);
    }

    @Override
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.getSkuSaleAttrValueListBySpu(spuId);
    }
}
