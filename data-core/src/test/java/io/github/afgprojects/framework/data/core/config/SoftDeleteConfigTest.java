package io.github.afgprojects.framework.data.core.config;

import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SoftDeleteConfig 测试
 */
@DisplayName("SoftDeleteConfig 测试")
class SoftDeleteConfigTest {

    @Nested
    @DisplayName("工厂方法测试")
    class FactoryMethodTests {

        @Test
        @DisplayName("defaults 应该返回默认配置")
        void defaultsShouldReturnDefaultConfig() {
            // When
            SoftDeleteConfig config = SoftDeleteConfig.defaults();

            // Then
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getStrategy()).isEqualTo(SoftDeleteStrategy.BOOLEAN);
            assertThat(config.isAutoFilterDeleted()).isTrue();
        }

        @Test
        @DisplayName("booleanStrategy 应该返回 Boolean 模式配置")
        void booleanStrategyShouldReturnBooleanConfig() {
            // When
            SoftDeleteConfig config = SoftDeleteConfig.booleanStrategy();

            // Then
            assertThat(config.getStrategy()).isEqualTo(SoftDeleteStrategy.BOOLEAN);
            assertThat(config.getFieldName()).isEqualTo("deleted");
        }

        @Test
        @DisplayName("timestampStrategy 应该返回时间戳模式配置")
        void timestampStrategyShouldReturnTimestampConfig() {
            // When
            SoftDeleteConfig config = SoftDeleteConfig.timestampStrategy();

            // Then
            assertThat(config.getStrategy()).isEqualTo(SoftDeleteStrategy.TIMESTAMP);
            assertThat(config.getFieldName()).isEqualTo("deletedAt");
        }
    }

    @Nested
    @DisplayName("字段名测试")
    class FieldNameTests {

        @Test
        @DisplayName("Boolean 模式应该返回 deleted 字段名")
        void booleanModeShouldReturnDeletedFieldName() {
            // Given
            SoftDeleteConfig config = SoftDeleteConfig.booleanStrategy();

            // When & Then
            assertThat(config.getFieldName()).isEqualTo("deleted");
        }

        @Test
        @DisplayName("时间戳模式应该返回 deletedAt 字段名")
        void timestampModeShouldReturnDeletedAtFieldName() {
            // Given
            SoftDeleteConfig config = SoftDeleteConfig.timestampStrategy();

            // When & Then
            assertThat(config.getFieldName()).isEqualTo("deletedAt");
        }

        @Test
        @DisplayName("应该支持自定义字段名")
        void shouldSupportCustomFieldName() {
            // Given
            SoftDeleteConfig config = new SoftDeleteConfig();
            config.setBooleanFieldName("is_deleted");
            config.setTimestampFieldName("deleted_time");

            // When & Then
            config.setStrategy(SoftDeleteStrategy.BOOLEAN);
            assertThat(config.getFieldName()).isEqualTo("is_deleted");

            config.setStrategy(SoftDeleteStrategy.TIMESTAMP);
            assertThat(config.getFieldName()).isEqualTo("deleted_time");
        }
    }

    @Nested
    @DisplayName("配置属性测试")
    class PropertyTests {

        @Test
        @DisplayName("应该支持禁用软删除")
        void shouldSupportDisableSoftDelete() {
            // Given
            SoftDeleteConfig config = SoftDeleteConfig.defaults();
            config.setEnabled(false);

            // When & Then
            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该支持禁用自动过滤")
        void shouldSupportDisableAutoFilter() {
            // Given
            SoftDeleteConfig config = SoftDeleteConfig.defaults();
            config.setAutoFilterDeleted(false);

            // When & Then
            assertThat(config.isAutoFilterDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("toString 应该包含关键配置信息")
        void toStringShouldContainKeyInfo() {
            // Given
            SoftDeleteConfig config = SoftDeleteConfig.timestampStrategy();

            // When
            String result = config.toString();

            // Then
            assertThat(result).contains("enabled=true");
            assertThat(result).contains("strategy=TIMESTAMP");
            assertThat(result).contains("fieldName='deletedAt'");
        }
    }

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("默认值常量应该正确")
        void defaultConstantsShouldBeCorrect() {
            // When & Then
            assertThat(SoftDeleteConfig.DEFAULT_STRATEGY).isEqualTo(SoftDeleteStrategy.BOOLEAN);
            assertThat(SoftDeleteConfig.DEFAULT_DELETED_VALUE).isTrue();
            assertThat(SoftDeleteConfig.DEFAULT_NOT_DELETED_VALUE).isFalse();
        }
    }
}
