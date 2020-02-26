package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {

    /**
     * 查询所有的数据
     * @return
     */
    List<UserInfo> findAll();

    /**
     * 根据用户id查询用户地址
     * @param userId
     * @return
     */
    List<UserAddress> getUserAddressList(String userId);


    /**
     * 登陆方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    UserInfo verify(String userId);
}
