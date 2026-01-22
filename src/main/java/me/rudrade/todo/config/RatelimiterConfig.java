package me.rudrade.todo.config;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

@Configuration
public class RatelimiterConfig {

    @Bean
    RedisClient redisClient() {
        return RedisClient.create(RedisURI.builder()
            .withHost("redis")
            .withPort(6379)
            .withSsl(false).build());
    }

    @Bean
    ProxyManager<String> proxyManager(RedisClient redisClient) {
        StatefulRedisConnection<String, byte[]> redisConnection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        return LettuceBasedProxyManager.builderFor(redisConnection).build();
    }

    @Bean
    Supplier<BucketConfiguration> bucketConfiguration() {
        return () -> BucketConfiguration.builder()
        .addLimit(limit ->  limit.capacity(30L).refillGreedy(30L, Duration.ofMinutes(1L)))
        .build();
    }

}
