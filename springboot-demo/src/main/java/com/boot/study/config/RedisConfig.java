package com.boot.study.config;

import com.boot.study.utils.RedisLockUtil;
import com.boot.study.utils.RedisUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.TransportMode;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // String的序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        //使用fastjson2 序列化
        FastJson2JsonRedisSerializer<Object> fastJsonSerializer = new FastJson2JsonRedisSerializer<>(Object.class);
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的ke也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);
        template.setHashValueSerializer(fastJsonSerializer);
        template.afterPropertiesSet();
        RedisUtil.init(template);
        return template;
    }

    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        config.setTransportMode(TransportMode.NIO);
//        config.setCodec(new FstCodec());
        SingleServerConfig singleServerConfig = config.useSingleServer();
        //可以用"rediss://"来启用SSL连接
        singleServerConfig.setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort());
        // 仅在密码非空时设置；Redisson 3.20.0 对空字符串密码仍会发送 AUTH 命令，
        // 而无密码的本机 Redis 会返回 "ERR Client sent AUTH, but no password is set"。
        String redisPassword = redisProperties.getPassword();
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }
        singleServerConfig.setDatabase(redisProperties.getDatabase());
        RedissonClient redissonClient = Redisson.create(config);
        RedisLockUtil.init(redissonClient);
        return redissonClient;
    }
}