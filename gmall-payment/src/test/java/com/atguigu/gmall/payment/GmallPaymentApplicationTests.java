package com.atguigu.gmall.payment;

import com.atguigu.gmall.config.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Test
    public void contextLoads() {
    }

     @Test
    public void testMq() throws JMSException {
         /**
          * 1. 创建连接工厂
          * 2. 创建连接
          * 3. 打开连接
          * 4. 创建session
          * 5. 创建队列
          * 6. 创建消息提供者
          * 7. 创建消息对象
          * 8. 发送消息
          * 9. 关闭
          */
         // 创建连接工厂
         Connection connection = activeMQUtil.getConnection();//创建连接
         connection.start();
         // 创建session 第一个参数表示是否支持事务，false时，第二个参数Session.AUTO_ACKNOWLEDGE，Session.CLIENT_ACKNOWLEDGE，DUPS_OK_ACKNOWLEDGE其中一个
         // 第一个参数设置为true时，第二个参数可以忽略 服务器设置为SESSION_TRANSACTED
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);//第二个参数表示开启或者关闭事物的相应参数
         // 创建队列
         Queue queue = session.createQueue("Atguigu-tools");

         MessageProducer producer = session.createProducer(queue);

         // 创建消息对象
         ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
         activeMQTextMessage.setText("可口可乐的生活，快乐的生活");

//        session.commit();//如果开启事物的话，那么这一行代码是必须的

         // 发送消息
         producer.send(activeMQTextMessage);
         producer.close();
         connection.close();
     }

}
