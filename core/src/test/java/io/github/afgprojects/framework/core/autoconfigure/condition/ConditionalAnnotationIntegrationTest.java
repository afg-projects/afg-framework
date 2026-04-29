package io.github.afgprojects.framework.core.autoconfigure.condition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import io.github.afgprojects.framework.core.support.RedisContainerSingleton;
import org.springframework.test.annotation.DirtiesContext;
import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * 条件装配集成测试
 * 验证条件注解在 Spring 容器中的实际行为
 */
@SpringBootTest(classes = {TestApplication.class, ConditionTestConfiguration.class})
@TestPropertySource(properties = {
    "afg.feature.cache.enabled=true",
    "afg.feature.search.enabled=false",
    "afg.tenant.id=tenant-001",
    "afg.database.url=jdbc:mysql://localhost:3306/test"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConditionalAnnotationIntegrationTest {

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", RedisContainerSingleton::getHost);
        registry.add("spring.data.redis.port", RedisContainerSingleton::getPort);
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("功能启用时 Bean 应该被装配")
    void shouldLoadBeanWhenFeatureEnabled() {
        // 执行并验证
        assertThat(applicationContext.containsBean("cacheService")).isTrue();
    }

    @Test
    @DisplayName("功能禁用时 Bean 不应该被装配")
    void shouldNotLoadBeanWhenFeatureDisabled() {
        // 执行并验证
        assertThat(applicationContext.containsBean("searchService")).isFalse();
    }

    @Test
    @DisplayName("租户匹配时 Bean 应该被装配")
    void shouldLoadBeanWhenTenantMatches() {
        // 执行并验证
        assertThat(applicationContext.containsBean("tenantSpecificService")).isTrue();
    }

    @Test
    @DisplayName("租户不匹配时 Bean 不应该被装配")
    void shouldNotLoadBeanWhenTenantNotMatches() {
        // 执行并验证
        assertThat(applicationContext.containsBean("otherTenantService")).isFalse();
    }

    @Test
    @DisplayName("属性非空时 Bean 应该被装配")
    void shouldLoadBeanWhenPropertyNotEmpty() {
        // 执行并验证
        assertThat(applicationContext.containsBean("databaseService")).isTrue();
    }

    @Test
    @DisplayName("属性为空时 Bean 不应该被装配")
    void shouldNotLoadBeanWhenPropertyEmpty() {
        // 执行并验证
        assertThat(applicationContext.containsBean("missingPropertyService")).isFalse();
    }
}