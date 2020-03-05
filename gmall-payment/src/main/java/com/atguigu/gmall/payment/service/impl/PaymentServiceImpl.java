package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;


    @Reference
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
        return paymentInfoMapper.selectOne(paymentInfoQuery);
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD) {
        //更新
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfoUPD,example);
    }

    @Override
    public boolean refund(String orderId) {
        // 通过orderId 获取数据
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        //alipay.trade.refund
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("refund_amount",orderInfo.getTotalAmount());
        map.put("refund_reason","不买了！");
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            // 更新状态！
            System.out.println("调用成功");
            return true;

        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public Map createNative(String orderId, String money) {
        /*
            1.  制作参数使用map
            2.  map 转换为xml 并发送支付接口
            3.  获取结果
         */
        HashMap<String, String> map = new HashMap<>();
        map.put("appid",appid);
        map.put("mch_id",partner);
        map.put("nonce_str", WXPayUtil.generateNonceStr());
        map.put("body","买衣服");
        map.put("out_trade_no",orderId);
        map.put("spbill_create_ip","127.0.0.1");
        map.put("total_fee",money);
        map.put("notify_url","http://www.weixin.qq.com/wxpay/pay.php");
        map.put("trade_type","NATIVE");
        try {
            // 生成xml ，以post 请求方式 发送给支付接口
            String xmlParam  = WXPayUtil.generateSignedXml(map, partnerkey);
            // 导入工具类：项目中
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            // 设置https 请求
            httpClient.setHttps(true);
            // 将xmlParam 发送到接口上
            httpClient.setXmlParam(xmlParam);
            // 以post 请求
            httpClient.post();

            // 获取结果：将结果集放入map 中！
            Map<String, String> resultMap=new HashMap<>();
            // 将结果集转换为map
            String result  = httpClient.getContent();
            Map<String, String> xmlToMap = WXPayUtil.xmlToMap(result);

            resultMap.put("code_url",xmlToMap.get("code_url"));
            resultMap.put("total_fee",money);
            resultMap.put("out_trade_no",orderId);
            // 将结果返回控制器
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            //打开连接
            connection.start();
            //创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");

            //创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_queue);

            //创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());
            activeMQMapMessage.setString("result",result);

            //发送消息
            producer.send(activeMQMapMessage);

            //提交
            session.commit();
            //关闭连接
            closeAll(connection, session, producer);
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }



    /**
     * 查询交易是否成功，根据 orderId 查询
     * @param paymentInfoQuery
     * @return
     */
    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {
        // 查询当前的支付信息
        PaymentInfo paymentInfo = getPaymentInfo(paymentInfoQuery);
        if (paymentInfo.getPaymentStatus()== PaymentStatus.PAID || paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED){
            return true;
        }
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "\"out_trade_no\":\""+paymentInfo.getOutTradeNo()+"\"" +
                "  }");
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            if ("TRADE_SUCCESS".equals(response.getTradeStatus())||"TRADE_FINISHED".equals(response.getTradeStatus())){
                //  IPAD
                System.out.println("支付成功");
                // 改支付状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                updatePaymentInfo(paymentInfo.getOutTradeNo(),paymentInfoUpd);
                sendPaymentResult(paymentInfo,"success");
                return true;
            }else {
                System.out.println("支付失败");
                return false;
            }

        } else {
            System.out.println("支付失败");
            return false;
        }

    }

    @Override
    public void closePayment(String orderId) {
        // 更新状态：
        // 第一个参数：表示要更新的值
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);

        // 第二个参数：example 按照什么条件更新
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",orderId);

        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

    private void closeAll(Connection connection, Session session, MessageProducer producer) throws JMSException {
        producer.close();
        session.close();
        connection.close();
    }


}
