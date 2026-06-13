package io.github.afgprojects.framework.core.api.enummanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalEnumRegistry")
class LocalEnumRegistryTest {

    enum UserStatus {
        ACTIVE(1, "激活"),
        INACTIVE(0, "停用");

        private final int code;
        private final String description;

        UserStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }
    }

    enum SimpleStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    enum Priority {
        LOW("low", "低优先级"),
        HIGH("high", "高优先级");

        private final String value;
        private final String label;

        Priority(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() { return value; }
        public String getLabel() { return label; }
    }

    private LocalEnumRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new LocalEnumRegistry();
    }

    @Nested
    @DisplayName("register - 自动推断字段名")
    class RegisterWithAutoInference {

        @Test
        @DisplayName("推断 code 为值字段、description 为标签字段")
        void shouldInferCodeAndDescriptionFields() {
            registry.register(UserStatus.class);

            EnumMetadata metadata = registry.getMetadata("UserStatus");
            assertThat(metadata).isNotNull();
            assertThat(metadata.getValueField()).isEqualTo("code");
            assertThat(metadata.getLabelField()).isEqualTo("description");
        }

        @Test
        @DisplayName("正确提取枚举项的值和标签")
        void shouldExtractEnumItemsWithInferredFields() {
            registry.register(UserStatus.class);

            EnumMetadata metadata = registry.getMetadata("UserStatus");
            assertThat(metadata.getItems()).hasSize(2);

            EnumItem activeItem = metadata.getItems().stream()
                    .filter(item -> "ACTIVE".equals(item.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(activeItem.getValue()).isEqualTo(1);
            assertThat(activeItem.getLabel()).isEqualTo("激活");
            assertThat(activeItem.getOrdinal()).isEqualTo(0);

            EnumItem inactiveItem = metadata.getItems().stream()
                    .filter(item -> "INACTIVE".equals(item.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(inactiveItem.getValue()).isEqualTo(0);
            assertThat(inactiveItem.getLabel()).isEqualTo("停用");
            assertThat(inactiveItem.getOrdinal()).isEqualTo(1);
        }

        @Test
        @DisplayName("推断 value 为值字段、label 为标签字段")
        void shouldInferValueAndLabelFields() {
            registry.register(Priority.class);

            EnumMetadata metadata = registry.getMetadata("Priority");
            assertThat(metadata).isNotNull();
            assertThat(metadata.getValueField()).isEqualTo("value");
            assertThat(metadata.getLabelField()).isEqualTo("label");
            assertThat(metadata.getItems().get(0).getValue()).isEqualTo("low");
            assertThat(metadata.getItems().get(0).getLabel()).isEqualTo("低优先级");
        }

        @Test
        @DisplayName("无匹配字段时使用 ordinal/name 默认策略")
        void shouldUseDefaultStrategy_whenNoMatchingFields() {
            registry.register(SimpleStatus.class);

            EnumMetadata metadata = registry.getMetadata("SimpleStatus");
            assertThat(metadata).isNotNull();
            assertThat(metadata.getValueField()).isEqualTo("ordinal");
            assertThat(metadata.getLabelField()).isEqualTo("name");

            assertThat(metadata.getItems()).hasSize(3);
            assertThat(metadata.getItems().get(0).getLabel()).isEqualTo("PENDING");
            assertThat(metadata.getItems().get(0).getValue()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("register - 手动指定字段")
    class RegisterWithManualFields {

        @Test
        @DisplayName("手动指定 valueField 和 labelField")
        void shouldUseManualFields() {
            registry.register(UserStatus.class, "code", "description");

            EnumMetadata metadata = registry.getMetadata("UserStatus");
            assertThat(metadata).isNotNull();
            assertThat(metadata.getValueField()).isEqualTo("code");
            assertThat(metadata.getLabelField()).isEqualTo("description");
        }
    }

    @Nested
    @DisplayName("inferValueField / inferLabelField")
    class InferFields {

        @Test
        @DisplayName("推断 code 字段为值字段")
        void shouldInferCodeAsValueField() {
            assertThat(registry.inferValueField(UserStatus.class)).isEqualTo("code");
        }

        @Test
        @DisplayName("推断 value 字段为值字段")
        void shouldInferValueAsValueField() {
            assertThat(registry.inferValueField(Priority.class)).isEqualTo("value");
        }

        @Test
        @DisplayName("无匹配字段时推断 ordinal")
        void shouldFallbackToOrdinal() {
            assertThat(registry.inferValueField(SimpleStatus.class)).isEqualTo("ordinal");
        }

        @Test
        @DisplayName("推断 description 字段为标签字段")
        void shouldInferDescriptionAsLabelField() {
            assertThat(registry.inferLabelField(UserStatus.class)).isEqualTo("description");
        }

        @Test
        @DisplayName("推断 label 字段为标签字段")
        void shouldInferLabelAsLabelField() {
            assertThat(registry.inferLabelField(Priority.class)).isEqualTo("label");
        }

        @Test
        @DisplayName("无匹配字段时推断 name")
        void shouldFallbackToName() {
            assertThat(registry.inferLabelField(SimpleStatus.class)).isEqualTo("name");
        }
    }

    @Nested
    @DisplayName("getMetadata")
    class GetMetadata {

        @Test
        @DisplayName("通过类名获取元数据")
        void shouldGetMetadataByName() {
            registry.register(UserStatus.class);

            EnumMetadata metadata = registry.getMetadata("UserStatus");
            assertThat(metadata).isNotNull();
            assertThat(metadata.getName()).isEqualTo("UserStatus");
        }

        @Test
        @DisplayName("通过类对象获取元数据")
        void shouldGetMetadataByClass() {
            registry.register(UserStatus.class);

            EnumMetadata metadata = registry.getMetadata(UserStatus.class);
            assertThat(metadata).isNotNull();
        }

        @Test
        @DisplayName("未注册的枚举返回 null")
        void shouldReturnNull_forUnregisteredEnum() {
            assertThat(registry.getMetadata("NonExistent")).isNull();
        }
    }

    @Nested
    @DisplayName("getAllMetadata")
    class GetAllMetadata {

        @Test
        @DisplayName("返回所有已注册的元数据")
        void shouldReturnAllMetadata() {
            registry.register(UserStatus.class);
            registry.register(SimpleStatus.class);

            assertThat(registry.getAllMetadata()).hasSize(2);
        }

        @Test
        @DisplayName("未注册任何枚举时返回空列表")
        void shouldReturnEmptyList_whenNoRegistered() {
            assertThat(registry.getAllMetadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getItems")
    class GetItems {

        @Test
        @DisplayName("获取指定枚举的项列表")
        void shouldReturnItemsForEnum() {
            registry.register(UserStatus.class);

            assertThat(registry.getItems("UserStatus")).hasSize(2);
        }

        @Test
        @DisplayName("未注册的枚举返回空列表")
        void shouldReturnEmptyList_forUnregisteredEnum() {
            assertThat(registry.getItems("NonExistent")).isEmpty();
        }
    }
}
