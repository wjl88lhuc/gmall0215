package com.atguigu.gmall.passport;

import com.atguigu.gmall.passport.config.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Test
    public void testJwt(){
        String key = "atguigu";
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",1001);
        map.put("nickName","admin");
        String salt="192.168.1.104";//当前服务器的ip地址
        String tocken = JwtUtil.encode(key, map, salt);
        System.out.println(tocken);

        System.out.println("**********************************");
        //解密Tocken: 只有salt与key都是正确的才可以解密
        Map<String, Object> maps = JwtUtil.decode(tocken, key, salt);
        System.out.println(maps);


    }

}
