package com.gec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/*
 * Redis 配置类(9.11 资料): 手工声明两个 RedisTemplate Bean。
 *
 * 为什么自定义, 不用 Spring Boot 自动配置的那个?
 *   自动配置的 RedisTemplate<Object,Object> 默认走 JDK 序列化, key 写进 Redis
 *   会带一串二进制前缀(如 \xac\xed...), redis-cli 里查 key / get 值都是乱码。
 *
 * 两个模板的分工(序列化器区别):
 *  1.myJdkTemp     —— key/value 都用 JdkSerializationRedisSerializer(Java 原生序列化)。
 *     要求对象 implements Serializable, 存二进制; 不丢类型信息,
 *     但 redis-cli 里几乎不可读, 换语言或改字段后容易反序列化失败。
 *  2.myJacksonTemp —— key 用 StringRedisSerializer(纯字符串, redis-cli 可读),
 *     value/hashValue 用 GenericJackson2JsonRedisSerializer(JSON 文本, 且带 @class 类型信息,
 *     反序列化时能还原成原对象)。
 *     商品发布缓存(GoodsBaseInfoVO)就存这个模板, redis-cli 里能看到明文 JSON。
 */
@Configuration
public class MyRedisConfig {

    /* {1}JDK 序列化模板: key/value 全部 Java 原生二进制 */
    @Bean(value="myJdkTemp")
    public RedisTemplate<String,Object> myJdkTemp(
        RedisConnectionFactory factory ){
        RedisTemplate<String, Object> temp = new RedisTemplate<>();
        temp.setConnectionFactory( factory );
        temp.setKeySerializer( new JdkSerializationRedisSerializer() );
        temp.setValueSerializer( new JdkSerializationRedisSerializer() );
        return temp;
    }

    /* {2}JSON 序列化模板: key=字符串, value=JSON 文本(由资料代码原样保留) */
    @Bean(value="myJacksonTemp")
    public RedisTemplate<String,Object> myJacksonTemp(
            RedisConnectionFactory factory ){
        RedisTemplate<String, Object> temp = new RedisTemplate<>();
        temp.setConnectionFactory( factory );
        RedisSerializer valserializer = makeGenericJackon2Serializer();
        RedisSerializer keySerializer = makeStringSerializer();
        temp.setKeySerializer( keySerializer );
        temp.setValueSerializer( valserializer );

        /* Hash 结构(HashOperations)也用同一套序列化器:
           保证 hash 的 field 是字符串、value 是 JSON, hgetall 出来同样是明文 */
        temp.setHashKeySerializer( keySerializer );
        temp.setHashValueSerializer( valserializer );
        return temp;
    }

    /* 造 JSON 序列化器: Generic 版会在 JSON 里附带 @class, 保证反序列化能还原成原类型 */
    public RedisSerializer makeGenericJackon2Serializer(){
        return new GenericJackson2JsonRedisSerializer();
    }
    /* 造字符串序列化器: key 在 redis-cli 中直接可读 */
    public RedisSerializer makeStringSerializer(){
        return new StringRedisSerializer();
    }
}
