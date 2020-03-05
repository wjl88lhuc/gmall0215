package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CarCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;


    /**
     * 添加购物车
     * @param request
     * @param response
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, Integer skuNum) {
        /**
         * 1. 查看购物车中是否有该商品
         *
         * 2. 如果购物车中有该商品，则数量 加 1 ， 否则直接添加即可
         */
        List<CartInfo> cartInfos = new ArrayList<>();
        boolean isExist = false;
        // 从Cookie中获取购物车数据
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        if (StringUtils.isNotEmpty(cookieValue)){
            cartInfos = JSON.parseArray(cookieValue, CartInfo.class);
            for (CartInfo cartInfo : cartInfos) {
                if (cartInfo.getSkuId().equals(skuId)){
                    //购物车中有该商品，则该商品数量加 1
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());//实时价格初始化
                    isExist = true;
                    break;
                }
            }
        }
        if (!isExist){// 如果购物车中没有该商品，那么就直接将该商品添加到购物车中即可
            SkuInfo skuInfo = manageService.getSkuInfoBySkuId(skuId);
            //添加到购物车中
            CartInfo cartInfo=new CartInfo();

            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfos.add(cartInfo);
        }

        //把购物车写入到cookie中
        String newCartJson = JSON.toJSONString(cartInfos);
        CookieUtil.setCookie(request,response,cookieCartName,newCartJson,COOKIE_CART_MAXAGE,true);
    }

    public List<CartInfo> getCartList(HttpServletRequest request) {
        //未登录集合
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        if (StringUtils.isEmpty(cookieValue)){
            return JSON.parseArray(cookieValue, CartInfo.class);
        }
        return null;
    }

    //删除购物车
    public void deleteCarCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        //直接将isChecked赋值给购物车即可
        List<CartInfo> cartList = getCartList(request);
        if (cartList != null && cartList.size() > 0){
            for (CartInfo cartInfo : cartList) {
                if (cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setIsChecked(isChecked);
                }
            }
        }
        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartList),COOKIE_CART_MAXAGE,true);
    }
}
