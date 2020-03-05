package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;


import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        //数据不完整: 总金额，订单状态，第三方交易编号，创建时间，过期时间，进程状态
        orderInfo.sumTotalAmount();
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        // 生成第三方支付编号
        String outTradeNo="PEIKING"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间: 默认是 1 天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());

        //进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        //只保存了一份订单
        orderInfoMapper.insertSelective(orderInfo);

        //订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //设置orderId
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedisFromPool();
        String tradeNoKey="user:"+userId+":tradeCode";
        //定义一个流水号
        String uuIdTradeNo = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey,3600 ,uuIdTradeNo);
        if (jedis != null){
            jedis.close();
        }
        return uuIdTradeNo;
    }

    /**
     *
     * @param userId  用于获取缓存中的流水号
     * @param tradeCodeNo 页面的流水号
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        String tradeNoKey="user:"+userId+":tradeCode";
        Jedis jedis = redisUtil.getJedisFromPool();
        String tradeNoInRedis = jedis.get(tradeNoKey);
        if (jedis != null){
            jedis.close();
        }
        return StringUtils.isEquals(tradeNoInRedis,tradeCodeNo);
    }

    /**
     * 订单提交之后删除缓存中的流水号
     * @param userId
     */
    @Override
    public void delTradeCode(String userId) {
        String tradeNoKey="user:"+userId+":tradeCode";
        Jedis jedis = redisUtil.getJedisFromPool();
        jedis.del(tradeNoKey);
        if (jedis != null){
            jedis.close();
        }
    }

    /**
     * 查询是否有足够的库存
     * @param skuId
     * @param skuNum
     * @return
     */
    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        //调用库存系统的接口: http://www.gware.com/hasStock?skuId=?&num=?
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    /**
     *
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(paid);
        orderInfo.setOrderStatus(paid.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        //创建消息工厂
        Connection connection = activeMQUtil.getConnection();
        String orderInfoJson = initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            //创建队列
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            //创建消息提供者
            MessageProducer producer = session.createProducer(order_result_queue);

            //创建消息对象
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();

            //orderInfo组成的json字符串
            activeMQTextMessage.setText(orderInfoJson);
            producer.send(activeMQTextMessage);

            //提交
            session.commit();

            //关闭
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        // 当前系统时间>过期时间 and 当前状态是未支付！
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("processStatus",ProcessStatus.UNPAID).andLessThan("expireTime",new Date());
        List<OrderInfo> orderInfoList = orderInfoMapper.selectByExample(example);
        return orderInfoList;
    }


    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 将订单状态改为关闭
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 关闭paymentInfo
        paymentService.closePayment(orderInfo.getId());
    }

    /**
     * 跟俊orderId将orderInfo变为orderInfo字符串
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);

        //将orderInfo中的有用数据保存到map中
        Map map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);
    }

    /**
     *
     * @param orderInfo
     * @return
     */
    private Map initWareOrder(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","测试用例");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
//        map.put("wareId",orderInfo.getWareId());//仓库id

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List detailList = new ArrayList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailList.add(detailMap);
        }
        map.put("details",detailList);
        return map;
    }
}
