package com.hhh.yunpicturebackend;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass=true)//aop代理增强
@EnableCaching
public class YunPictureBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(YunPictureBackendApplication.class, args);
    }

}