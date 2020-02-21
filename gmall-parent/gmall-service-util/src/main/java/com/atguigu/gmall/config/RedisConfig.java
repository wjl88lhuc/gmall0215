package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    //表示如果未从配置文件中获取host,则使用默认值disbale
    @Value("${spring.redis.host:disbale}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    //默认 0 号库,如果未从配置文件中获取，则使用默认值
    @Value("${spring.redis.database:0}")
    private int database;

    //将获取的数据传入到RedisUtil 的 initJedisPool 方法中
    @Bean
    public RedisUtil getRedisUtil(){
        if ("disbale".equals(host) || 0 == port){
            return null;
        }
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host,port,database);
        return redisUtil;
    }

}
