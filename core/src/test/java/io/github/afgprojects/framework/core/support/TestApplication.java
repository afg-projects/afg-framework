package io.github.afgprojects.framework.core.support;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * 集成测试应用配置
 * 提供 Spring Boot 测试上下文所需的配置类
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class
})
public class TestApplication {}
