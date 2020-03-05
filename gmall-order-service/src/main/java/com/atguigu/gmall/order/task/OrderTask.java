package com.atguigu.gmall.order.task;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@EnableScheduling  //开启定时任务，加了这个配置到容器中，就自动马上执行定时任务
@Component
public class OrderTask {//关闭过期 订单

    @Reference
    private OrderService orderService;

    //表示任务启动规则
    @Scheduled(cron = "")
    @Scheduled(cron = "0/20 * * * * ?")
    public  void checkOrder(){
        System.out.println("开始处理过期订单");
        long starttime = System.currentTimeMillis();
        //查询过期订单（当前时间大于过期时间而且订单的状态是未支付的订单就是过期订单）
        List<OrderInfo> expiredOrderList = orderService.getExpiredOrderList();
        for (OrderInfo orderInfo : expiredOrderList) {
            // 循环处理未完成订单
            orderService.execExpiredOrder(orderInfo);
        }
        long costtime = System.currentTimeMillis() - starttime;
        System.out.println("一共处理"+expiredOrderList.size()+"个订单 共消耗"+costtime+"毫秒");
    }



}
