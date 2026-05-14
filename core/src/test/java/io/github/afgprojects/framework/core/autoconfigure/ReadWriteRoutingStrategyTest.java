package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ReadWriteRoutingStrategy 单元测试。
 * 测试读写分离路由策略的路由逻辑和负载均衡功能。
 *
 * @see ReadWriteRoutingStrategy
 */
@DisplayName("ReadWriteRoutingStrategy 测试")
class ReadWriteRoutingStrategyTest {

    /**
     * 路由策略测试。
     * 验证读写操作的数据源路由逻辑。
     */
    @Nested
    @DisplayName("路由策略测试")
    class RoutingTests {

        /**
         * 测试写操作路由到写数据源。
         */
        @Test
        @DisplayName("写操作应该路由到写数据源")
        void shouldRouteWriteToWriteDatasource() {
            ReadWriteRoutingStrategy strategy = new ReadWriteRoutingStrategy(
                    "master",
                    List.of("slave1", "slave2")
            );

            assertThat(strategy.getDatasource(ReadWriteRoutingStrategy.OperationType.WRITE))
                    .isEqualTo("master");
        }

        /**
         * 测试读操作路由到读数据源。
         */
        @Test
        @DisplayName("读操作应该路由到读数据源")
        void shouldRouteReadToReadDatasource() {
            ReadWriteRoutingStrategy strategy = new ReadWriteRoutingStrategy(
                    "master",
                    List.of("slave1", "slave2")
            );

            assertThat(strategy.getDatasource(ReadWriteRoutingStrategy.OperationType.READ))
                    .isIn("slave1", "slave2");
        }

        /**
         * 测试没有读数据源时使用写数据源。
         */
        @Test
        @DisplayName("没有读数据源时应该使用写数据源")
        void shouldUseWriteDatasourceWhenNoReadDatasources() {
            ReadWriteRoutingStrategy strategy = new ReadWriteRoutingStrategy(
                    "master",
                    List.of()
            );

            assertThat(strategy.getDatasource(ReadWriteRoutingStrategy.OperationType.READ))
                    .isEqualTo("master");
        }
    }

    /**
     * 负载均衡测试。
     * 验证读数据源的负载均衡逻辑。
     */
    @Nested
    @DisplayName("负载均衡测试")
    class LoadBalanceTests {

        /**
         * 测试单个读数据源直接返回。
         */
        @Test
        @DisplayName("单个读数据源应该直接返回")
        void shouldReturnSingleReadDatasource() {
            ReadWriteRoutingStrategy strategy = new ReadWriteRoutingStrategy(
                    "master",
                    List.of("slave1")
            );

            assertThat(strategy.getDatasource(ReadWriteRoutingStrategy.OperationType.READ))
                    .isEqualTo("slave1");
        }

        /**
         * 测试多个读数据源轮询。
         */
        @Test
        @DisplayName("多个读数据源应该轮询")
        void shouldRotateReadDatasources() {
            ReadWriteRoutingStrategy strategy = new ReadWriteRoutingStrategy(
                    "master",
                    List.of("slave1", "slave2", "slave3")
            );

            String first = strategy.getDatasource(ReadWriteRoutingStrategy.OperationType.READ);
            String second = strategy.getDatasource(ReadWriteRoutingStrategy.OperationType.READ);
            String third = strategy.getDatasource(ReadWriteRoutingStrategy.OperationType.READ);
            String fourth = strategy.getDatasource(ReadWriteRoutingStrategy.OperationType.READ);

            // 验证轮询
            assertThat(fourth).isEqualTo(first);
        }
    }

    /**
     * OperationType 枚举测试。
     * 验证操作类型枚举的完整性。
     */
    @Nested
    @DisplayName("OperationType 枚举测试")
    class OperationTypeTests {

        /**
         * 测试枚举包含 READ 和 WRITE。
         */
        @Test
        @DisplayName("应该包含 READ 和 WRITE")
        void shouldContainReadAndWrite() {
            ReadWriteRoutingStrategy.OperationType[] types = ReadWriteRoutingStrategy.OperationType.values();

            assertThat(types).hasSize(2);
            assertThat(types).contains(
                    ReadWriteRoutingStrategy.OperationType.READ,
                    ReadWriteRoutingStrategy.OperationType.WRITE
            );
        }
    }
}
