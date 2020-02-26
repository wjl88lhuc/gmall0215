package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
//import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return  userAddressMapper.select(userAddress); // select * from user_address where userId = xxx
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //对密码加密
        String passwd = userInfo.getPasswd();
        String md5Password = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(md5Password);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if (info != null){
            Jedis jedis = redisUtil.getJedisFromPool();
            //放入到redis中,给key起名，key的名字： user:userId:info
            String userKey = userKey_prefix + info.getId() + userinfoKey_suffix;
            //setex 存入到redis中，并且给key设置过期时间
            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));
            jedis.close();//关闭redis连接
            return info;
        }
        return null; // 如果查询不到就直接返回空即可。
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = null;
        UserInfo info = null;
        try {
            jedis = redisUtil.getJedisFromPool();
            String key = userKey_prefix + userId + userinfoKey_suffix;
            String userJson = jedis.get(key);
            if (!StringUtils.isEmpty(userJson)){
                //转换成为对象
                info = JSON.parseObject(userJson, UserInfo.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null){
                jedis.close();
            }
            return info;
        }
    }


}
