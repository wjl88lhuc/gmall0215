package com.atguigu.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    //创建一个redis连接池
    private JedisPool jedisPool;

    //初始化连接池
    public void initJedisPool(String host,int port,int database){
        //创建jedis配置类
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //设置连接池的最大连接数
        jedisPoolConfig.setMaxTotal(200);

        //设置连接池的最大等待时间
        jedisPoolConfig.setMaxWaitMillis(10 * 1000);

        //设置最小剩余数
        jedisPoolConfig.setMinIdle(10);

        // true表示当一个用户获取一个连接之后，自测是否可以使用连接
        jedisPoolConfig.setTestOnBorrow(true);

        //true 表示 当连接池中的连接用完之后，如果还有需要新的连接请求，那么就等待
        jedisPoolConfig.setBlockWhenExhausted(true);

        jedisPool = new JedisPool(jedisPoolConfig,host,port,20 * 1000);
    }

    /**
     * 从连接池中获取jedis客户端连接
     * @return
     */
    public Jedis getJedisFromPool(){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return jedis;
        }
    }


}
