package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

import java.util.List;

public interface OrderService {

    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    public  String getTradeNo(String userId);

    /**
     * 验证流水号
     * @param userId  用于获取缓存中的流水号
     * @param tradeCodeNo 页面的流水号
     * @return
     */
    public  boolean checkTradeCode(String userId,String tradeCodeNo);

    /**
     * 删除流水号
     * @param userId
     */
    public void  delTradeCode(String userId);

    /**
     * 验证库存,即查询是否有足够的库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 通过orderId获取订单对象
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 更新订单状态
     * @param orderId
     * @param paid
     */
    void updateOrderStatus(String orderId, ProcessStatus paid);

    /**
     * 发送消息给库存
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 查询过期订单
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期订单
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);
}
