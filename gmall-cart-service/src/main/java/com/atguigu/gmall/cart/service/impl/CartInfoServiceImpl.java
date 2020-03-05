package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Reference
    private ManageService manageService;

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /**
         * 1. 如果购物车中有相同的商品，则商品数量增加
         * 2. 如果没有，则直接添加到数据库中即可
         * 3. 更新缓存
         */
        //通过查询数据库中的商品，根据skuId与userId
        // select * from cartInfo where userId = ? and skuId = ?
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);
        if (cartInfoExist != null){
            //如果有相同的商品，则数量加 1
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            // 给skuPrice初始化操作， skuPrice = carPrice
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
//            cartInfoMapper.insertSelective(cartInfoExist);
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        }else{
            //如果数据库中这个用户没有添加这个商品，则直接保存即可
            SkuInfo skuInfo = manageService.getSkuInfoBySkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuNum(skuNum);
            //添加到数据库
//            cartInfoMapper.insert(cartInfo);
            cartInfoMapper.insertSelective(cartInfo);
            cartInfoExist=cartInfo;
        }
        Jedis jedis = redisUtil.getJedisFromPool();
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //缓存更新
        String cartInfoJsonString = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey,skuId,cartInfoJsonString);
        //更新购物车的过期时间，让购物车的过期时间与用户的过期时间是一样的即可
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long userInfoKeyTime = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,userInfoKeyTime.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        /**
         * 1. 如果购物车在缓存中存在，则看购物车
         * 2. 如果缓存中没有购物车，则先看数据库，然后再将数据放入到缓存中即可
         */
        //1. 缓存中获取
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<CartInfo> cartInfoList = new ArrayList<>();
        //从key中获取数据
        Jedis jedis =redisUtil.getJedisFromPool();
        List<String> cartInfos = jedis.hvals(userCartKey);
        if (cartInfos != null && cartInfos.size() > 0){
            cartInfoList = new ArrayList<>();
            for (String cartInfo : cartInfos) {
                cartInfoList.add(JSON.parseObject(cartInfo,CartInfo.class));
            }
            //真实业务中是需要按照时间来排序的
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
        }else{
            //缓存中没有该用户的购物车，则先从数据库中查找
            cartInfoList = loadCartCache(userId,jedis,userCartKey);
        }
        jedis.close();
        return cartInfoList;
    }

    /**
     *
     * @param cartListCK cookie中的购物车
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        //根据userId获取购物车数据
        List<CartInfo> cartInfoListDb = cartInfoMapper.selectCartListWithCurPrice(userId);//数据库中的购物车信息
        if (cartListCK != null  && cartListCK.size() > 0){
            boolean isExistInDb = false;//cookie购物车的中的skuId对应的商品在数据库购物车中是否存在，默认不存在
            for (CartInfo cartInfoCk : cartListCK) {
                for (CartInfo cartInfo : cartInfoListDb) {
                    if (cartInfoCk.getSkuId().equals(cartInfo.getSkuId())){
                        cartInfo.setSkuNum(cartInfoCk.getSkuNum() + cartInfo.getSkuNum());
                        //修改数据
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
                        isExistInDb = true;
                    }
                }
                if (!isExistInDb){
                    //如果cookie中的商品在数据库中不存在，那么就直接将cookie中的商品添加到数据库中的购物车即可
                    //将用户id赋值给未登录对象
                    cartInfoCk.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoCk);
                }
                isExistInDb = false;
            }
        }

        // 定义购物车的key=user:userId:cart  用户key=user:userId:info
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedisFromPool();
        List<CartInfo> cartInfos = loadCartCache(userId,jedis,cartKey);

        //与 未登录合并
        if (cartListCK != null && cartListCK.size() > 0){
            for (CartInfo cartInfoDb : cartInfos) {
                for (CartInfo infoCk : cartListCK) {
                    if (cartInfoDb.getSkuId().equals(infoCk.getSkuId())){
                        if ("1".equals(infoCk.getIsChecked())){
                            cartInfoDb.setIsChecked(infoCk.getIsChecked());
                            //修改数据库的状态
                            checkCart(cartInfoDb.getSkuId(),"1",userId);
                        }
                    }
                }
            }
        }
//        else {
//            for (CartInfo cartInfoDb : cartInfos) {
//                if ("1".equals(cartInfoDb.getIsChecked())){
//                    checkCart(cartInfoDb.getSkuId(),"1",userId);
//                }
//            }
//        }

        if (jedis != null){
            jedis.close();
        }
        return cartInfos;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        /**
         * 1. 获取redis客户端
         * 2. 获取购物车集合
         * 3. 直接修改skuId商品的勾选状态 isChecked
         * 4. 写回购物车
         * 5. 新建一个购物车来存储勾选的商品
         */
        List<CartInfo> isCheckedCartInfos = new ArrayList<>();
        Jedis jedis = redisUtil.getJedisFromPool();
        // 定义购物车的key=user:userId:cart  用户key=user:userId:info
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String cartInfoJson = jedis.hget(cartKey, skuId);//根据skuId字段获取 cartKey对应的redis中的hash对象
        CartInfo cartInfo = new CartInfo();
        if (StringUtils.isNotEmpty(cartInfoJson)){
            cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            cartInfo.setIsChecked(isChecked);
            //写回购物车
            jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));
        }

        //新建一个购物车key: user:userId:checked
        String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        //isChecked = "1"是勾选商品
        if ("1".equals(isChecked)){
            jedis.hset(cartCheckedKey,skuId,JSON.toJSONString(cartInfo));
        }else{
            //删除被取消勾选的商品
            jedis.hdel(cartCheckedKey,skuId);
        }
        if (jedis != null){
            jedis.close();
        }
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //1. 获取被勾选的购物车数据集合
        Jedis jedis = redisUtil.getJedisFromPool();
        //被选中的购物车
        String cartKeyChecked = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

        List<String> hvals = jedis.hvals(cartKeyChecked);

        //循环判断
        if (hvals != null && hvals.size() > 0){
            for (String cartInfoJson : hvals) {
                cartInfoList.add(JSON.parseObject(cartInfoJson,CartInfo.class));
            }
        }
        if (jedis != null){
            jedis.close();
        }
        return cartInfoList;
    }

    @Override
    public List<CartInfo> loadCartCache(String userId) {
        Jedis jedis = redisUtil.getJedisFromPool();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<CartInfo> result = loadCartCache(userId,jedis,userCartKey);
        if (jedis != null){
            jedis.close();
        }
        return result;
    }

    public List<CartInfo> loadCartCache(String userId,Jedis jedis,String userCartKey) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList == null || cartInfoList.size() == 0){
            return null;
        }
        //从数据库中查询到数据库，然后放到缓存redis中
        for (CartInfo cartInfo : cartInfoList) {
            jedis.hset(userCartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        return cartInfoList;
    }
}
