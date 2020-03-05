package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    private CartInfoService cartInfoService;

    @Reference
    private OrderService orderService;

    @Reference
    private UserService userService;

    @Reference
    private ManageService manageService;

//    @RequestMapping("trade")
//    public String trade(){
//        //返回一个视图名称
//        return "index";
//    }

    @RequestMapping("trade")
//    @ResponseBody
    @LoginRequire
    public String trade(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<UserAddress> userAddressList = userService.getUserAddressList(userId);
        request.setAttribute("userAddressList", userAddressList);

        //展示送货清单
        //数据来源：勾选的购物车 user:userId:checked
        List<CartInfo> cartInfoList = cartInfoService.getCartCheckedList(userId);

        // 订单信息集合
        List<OrderDetail> orderDetailList = new ArrayList<>(cartInfoList.size());

        //将集合数据赋值给OrderDetail
        if (cartInfoList != null && cartInfoList.size() > 0) {
            for (CartInfo cartInfo : cartInfoList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetailList.add(orderDetail);
            }
        }
        //保存用户清单集合
        request.setAttribute("orderDetailList", orderDetailList);

        //总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        //计算总金额
        orderInfo.sumTotalAmount();
        //保存总金额
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());

        //生成流水号
        String tradeNo = orderService.getTradeNo(userId);//流水号
        request.setAttribute("tradeNo",tradeNo);
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    //http://order.gmall.com/submitOrder
    public String submitOrder(HttpServletRequest request, OrderInfo orderInfo) {

        String userId = (String) request.getAttribute("userId");
        //判断是否重复提交
        String tradeNo = (String)request.getParameter("tradeNo");//前端页面传递过来的流水号
        boolean checkTradeCodeResult = orderService.checkTradeCode(userId, tradeNo);
        if (!checkTradeCodeResult){
            //是重复提交
            request.setAttribute("errMsg","该订单已经提交过了，请勿重复提交");
            return "tradeFail";//跳转到错误页面（已经重复提交的页面）
        }

        orderInfo.setUserId(userId);// orderInfo中还缺少

        //验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean flag = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!flag){
                request.setAttribute("errMsg",orderDetail.getSkuName() + "商品库存不足");
                return "tradeFail";//跳转到错误页面（已经重复提交的页面）
            }
            SkuInfo skuInfo = manageService.getSkuInfoBySkuId(orderDetail.getSkuId());
            int result = skuInfo.getPrice().compareTo(orderDetail.getOrderPrice());
            if (result != 0 ){//订单显示的商品价格与商品在数据库中的价格不一致
                request.setAttribute("errMsg",orderDetail.getSkuName() + "价格不匹配");
                cartInfoService.loadCartCache(userId);//查询实时价格，并且更新缓存
                return "tradeFail";//跳转到错误页面（已经重复提交的页面）
            }
        }

        //调用服务层
        String orderId = orderService.saveOrder(orderInfo);

        //删除流水中的流水号
        orderService.delTradeCode(userId);
        //支付
        return "redirect://payment.gmall.com/index?orderId=" + orderId;
    }

}
