package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CarController {

    @Reference
    private CartInfoService cartInfoService;

    @Autowired
    private CarCookieHandler carCookieHandler;

    @Reference
    private ManageService manageService;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        //获取商品数量
        String skuNum = request.getParameter("skuNum");
        //获取商品 skuId
        String skuId = request.getParameter("skuId");
        if (userId != null){
            //调用登陆添加购物车
            cartInfoService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else{
            //调用未登录添加购物车,说明用户没有登录没有登录放到cookie中
            carCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
        }

        //根据skuId查询 skuInfo
        SkuInfo skuInfo = manageService.getSkuInfoBySkuId(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);

        return "success";
    }

//http://cart.gmall.com/cartList
    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        //获取用户 userId
//        String userId =(String)request.getAttribute("userId");
        String userId = (String) request.getAttribute("userId");
        String sb = request.getAttributeNames().toString();
        System.out.println("sb: " +sb);
        List<CartInfo> cartInfoList = null;
        if (userId != null){
            List<CartInfo> cartListCK=carCookieHandler.getCartList(request);//查询未登陆状态下的购物车
//            if (cartListCK != null && cartListCK.size() > 0){
//                //合并购物车
//                cartInfoList = cartInfoService.mergeToCartList(cartListCK,userId);
//            }else{
//                //已登陆状态下查询购物车
//                cartInfoList=cartInfoService.getCartList(userId);
//            }
            //将cookie中的购物车与数据库中的购物车进行合并
            cartInfoList = cartInfoService.mergeToCartList(cartListCK,userId);
            //删除未登陆购物车
            carCookieHandler.deleteCarCookie(request,response);
        }else{
            //未登陆
            cartInfoList=carCookieHandler.getCartList(request);
        }
        request.setAttribute("cartInfoList",cartInfoList);


        return "cartList";
    }

    //http://cart.gmall.com/checkCart
    @RequestMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        //获取页面传递过来的数据
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        String userId = (String) request.getAttribute("userId");
        if (userId != null){
            //登陆状态
            cartInfoService.checkCart(skuId,isChecked,userId);
        }else{
            //未登录状态
            carCookieHandler.checkCart(request,response,skuId,isChecked);
        }

    }

    //http://cart.gmall.com/toTrade
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        //合并勾选的商品: 登陆 + 未登录
        List<CartInfo> cartList = carCookieHandler.getCartList(request);
        String userId = (String)request.getAttribute("userId");
        if (cartList != null && cartList.size() > 0){
            //合并勾选的商品: 登陆 + 未登录
            cartInfoService.mergeToCartList(cartList, userId);
            //删除未登录数据
            carCookieHandler.deleteCarCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }



}
