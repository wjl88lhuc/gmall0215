package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartInfoService  {
    void  addToCart(String skuId,String userId,Integer skuNum);


    /**
     * 登陆时候，根据用户id查询
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     * 修改商品状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据用户userId 查询已经勾选的购物车
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     *通过userId查询实时价格
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
