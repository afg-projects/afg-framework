package io.github.afgprojects.framework.starter.transaction;

import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TransactionAutoConfiguration 单元测试
 * <p>
 * 测试场景：
 * <ul>
 *   <li>当 PlatformTransactionManager 存在时，自动配置 SpringTransactionAdapter</li>
 *   <li>当用户自定义 TransactionAdapter 时，自动配置退避</li>
 *   <li>当 PlatformTransactionManager 不存在时，不触发自动配置</li>
 * </ul>
 */
@DisplayName("TransactionAutoConfiguration 测试")
class TransactionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TransactionAutoConfiguration.class));

    /**
     * 创建一个简单的测试用 PlatformTransactionManager
     */
    private static PlatformTransactionManager createTestTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return Mockito.mock(TransactionStatus.class);
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
                // no-op
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
                // no-op
            }
        };
    }

    @Test
    @DisplayName("当 PlatformTransactionManager 存在时，应注册 SpringTransactionAdapter Bean")
    void whenPlatformTransactionManagerExists_thenSpringTransactionAdapterShouldBeCreated() {
        contextRunner
                .withBean(PlatformTransactionManager.class, TransactionAutoConfigurationTest::createTestTransactionManager)
                .run(context -> {
                    assertThat(context).hasSingleBean(SpringTransactionAdapter.class);
                    assertThat(context).hasSingleBean(TransactionAdapter.class);

                    SpringTransactionAdapter adapter = context.getBean(SpringTransactionAdapter.class);
                    assertThat(adapter.getTransactionManager()).isNotNull();
                    assertThat(adapter.getTransactionTemplate()).isNotNull();
                });
    }

    @Test
    @DisplayName("当用户自定义 TransactionAdapter 时，自动配置应退避")
    void whenCustomTransactionAdapterExists_thenAutoConfigurationShouldBackOff() {
        contextRunner
                .withBean(PlatformTransactionManager.class, TransactionAutoConfigurationTest::createTestTransactionManager)
                .withBean(TransactionAdapter.class, () -> new TransactionAdapter() {
                    @Override
                    public void executeInTransaction(Runnable action) {
                        action.run();
                    }

                    @Override
                    public <T> T executeInTransaction(java.util.function.Supplier<T> action) {
                        return action.get();
                    }

                    @Override
                    public <T> T executeInReadOnly(java.util.function.Supplier<T> action) {
                        return action.get();
                    }
                })
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SpringTransactionAdapter.class);
                    assertThat(context).hasSingleBean(TransactionAdapter.class);
                });
    }

    @Test
    @DisplayName("当 PlatformTransactionManager 不存在时，不应注册 SpringTransactionAdapter")
    void whenPlatformTransactionManagerDoesNotExist_thenSpringTransactionAdapterShouldNotBeCreated() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(SpringTransactionAdapter.class);
            assertThat(context).doesNotHaveBean(TransactionAdapter.class);
        });
    }

    @Test
    @DisplayName("SpringTransactionAdapter 应正确注入 PlatformTransactionManager")
    void springTransactionAdapterShouldInjectPlatformTransactionManager() {
        contextRunner
                .withBean(PlatformTransactionManager.class, TransactionAutoConfigurationTest::createTestTransactionManager)
                .run(context -> {
                    SpringTransactionAdapter adapter = context.getBean(SpringTransactionAdapter.class);
                    PlatformTransactionManager manager = context.getBean(PlatformTransactionManager.class);

                    assertThat(adapter.getTransactionManager()).isSameAs(manager);
                });
    }
}
