package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    //获取消息队列中的数据
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public  void  consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        if ("success".equals(result)){
            //更新订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);

            //发送消息给库存
            orderService.sendOrderStatus(orderId);
            //更新订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);

        }

    }
}
