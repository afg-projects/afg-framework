package io.github.afgprojects.framework.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * JacksonUtils 工具类测试。
 * <p>
 * 测试 JSON 序列化、反序列化、对象与 Map 互转、深拷贝等功能。
 *
 * @see JacksonUtils
 */
@DisplayName("JacksonUtils 测试")
class JacksonUtilsTest extends BaseUnitTest {

    /**
     * 测试用简单类，用于验证序列化和反序列化功能。
     */
    static class TestUser {
        private String name;
        private Integer age;

        public TestUser() {}

        public TestUser(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestUser testUser = (TestUser) o;
            return java.util.Objects.equals(name, testUser.name) && java.util.Objects.equals(age, testUser.age);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age);
        }
    }

    /**
     * 带时间字段的测试类，用于验证 Java 8 日期时间类型的序列化和反序列化。
     */
    static class TestEvent {
        private String title;
        private LocalDateTime eventTime;

        public TestEvent() {}

        public TestEvent(String title, LocalDateTime eventTime) {
            this.title = title;
            this.eventTime = eventTime;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public LocalDateTime getEventTime() {
            return eventTime;
        }

        public void setEventTime(LocalDateTime eventTime) {
            this.eventTime = eventTime;
        }
    }

    /**
     * 测试 toJson 方法正确序列化对象。
     * <p>
     * 验证对象能被正确转换为 JSON 字符串，字段值正确输出。
     */
    @Test
    @DisplayName("toJson - 应该正确序列化对象")
    void toJson_shouldSerializeObject() {
        TestUser user = new TestUser("张三", 25);
        String json = JacksonUtils.toJson(user);

        assertThat(json).contains("\"name\":\"张三\"");
        assertThat(json).contains("\"age\":25");
    }

    /**
     * 测试 toJson 方法忽略 null 值。
     * <p>
     * 验证序列化时值为 null 的字段不会出现在 JSON 输出中。
     */
    @Test
    @DisplayName("toJson - null 值应该被忽略")
    void toJson_nullValueShouldBeIgnored() {
        TestUser user = new TestUser("张三", null);
        String json = JacksonUtils.toJson(user);

        assertThat(json).contains("\"name\":\"张三\"");
        assertThat(json).doesNotContain("age");
    }

    /**
     * 测试 toJson 方法正确序列化 LocalDateTime。
     * <p>
     * 验证 Java 8 日期时间类型能被正确序列化为 ISO-8601 格式字符串。
     */
    @Test
    @DisplayName("toJson - 应该正确序列化 LocalDateTime")
    void toJson_shouldSerializeLocalDateTime() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        TestEvent event = new TestEvent("会议", now);
        String json = JacksonUtils.toJson(event);

        assertThat(json).contains("\"title\":\"会议\"");
        // JavaTimeModule 使用 ISO 格式序列化 LocalDateTime
        assertThat(json).contains("\"eventTime\":\"2024-01-15T10:30:45\"");
    }

    /**
     * 测试 parse 方法正确反序列化为对象。
     * <p>
     * 验证 JSON 字符串能被正确解析为指定类型的对象。
     */
    @Test
    @DisplayName("parse - 应该正确反序列化为对象")
    void parse_shouldDeserializeToObject() {
        String json = "{\"name\":\"李四\",\"age\":30}";
        TestUser user = JacksonUtils.parse(json, TestUser.class);

        assertThat(user.getName()).isEqualTo("李四");
        assertThat(user.getAge()).isEqualTo(30);
    }

    /**
     * 测试 parse 方法支持单引号 JSON。
     * <p>
     * 验证解析器能正确处理使用单引号的 JSON 字符串。
     */
    @Test
    @DisplayName("parse - 应该支持单引号 JSON")
    void parse_shouldSupportSingleQuotes() {
        String json = "{'name':'王五','age':28}";
        TestUser user = JacksonUtils.parse(json, TestUser.class);

        assertThat(user.getName()).isEqualTo("王五");
        assertThat(user.getAge()).isEqualTo(28);
    }

    /**
     * 测试 parse 方法忽略未知属性。
     * <p>
     * 验证 JSON 中包含目标类不存在的字段时，反序列化不会抛出异常。
     */
    @Test
    @DisplayName("parse - 应该忽略未知属性")
    void parse_shouldIgnoreUnknownProperties() {
        String json = "{\"name\":\"赵六\",\"age\":35,\"unknownField\":\"value\"}";
        TestUser user = JacksonUtils.parse(json, TestUser.class);

        assertThat(user.getName()).isEqualTo("赵六");
        assertThat(user.getAge()).isEqualTo(35);
    }

    /**
     * 测试 parse 方法对无效 JSON 抛出异常。
     * <p>
     * 验证解析无效 JSON 字符串时会抛出 RuntimeException。
     */
    @Test
    @DisplayName("parse - 无效 JSON 应该抛出异常")
    void parse_invalidJsonShouldThrowException() {
        String invalidJson = "not a valid json";

        assertThatThrownBy(() -> JacksonUtils.parse(invalidJson, TestUser.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize JSON to object");
    }

    /**
     * 测试 parseList 方法正确反序列化为 List。
     * <p>
     * 验证 JSON 数组能被正确解析为指定元素类型的 List。
     */
    @Test
    @DisplayName("parseList - 应该正确反序列化为 List")
    void parseList_shouldDeserializeToList() {
        String json = "[{\"name\":\"用户1\",\"age\":20},{\"name\":\"用户2\",\"age\":25}]";
        List<TestUser> users = JacksonUtils.parseList(json, TestUser.class);

        assertThat(users).hasSize(2);
        assertThat(users.get(0).getName()).isEqualTo("用户1");
        assertThat(users.get(0).getAge()).isEqualTo(20);
        assertThat(users.get(1).getName()).isEqualTo("用户2");
        assertThat(users.get(1).getAge()).isEqualTo(25);
    }

    /**
     * 测试 parseList 方法处理空数组。
     * <p>
     * 验证空 JSON 数组能被正确解析为空 List。
     */
    @Test
    @DisplayName("parseList - 空数组应该返回空 List")
    void parseList_emptyArrayShouldReturnEmptyList() {
        String json = "[]";
        List<TestUser> users = JacksonUtils.parseList(json, TestUser.class);

        assertThat(users).isEmpty();
    }

    /**
     * 测试 parseMap 方法正确反序列化为 Map。
     * <p>
     * 验证 JSON 对象能被正确解析为 Map&lt;String, Object&gt;。
     */
    @Test
    @DisplayName("parseMap - 应该正确反序列化为 Map")
    void parseMap_shouldDeserializeToMap() {
        String json = "{\"name\":\"测试\",\"age\":30,\"active\":true}";
        Map<String, Object> map = JacksonUtils.parseMap(json);

        assertThat(map.get("name")).isEqualTo("测试");
        assertThat(map.get("age")).isEqualTo(30);
        assertThat(map.get("active")).isEqualTo(true);
    }

    /**
     * 测试 parseMap 方法处理嵌套对象。
     * <p>
     * 验证 JSON 中的嵌套对象能被正确解析为嵌套的 Map。
     */
    @Test
    @DisplayName("parseMap - 嵌套对象应该转为 Map")
    void parseMap_nestedObjectShouldBeMap() {
        String json = "{\"name\":\"测试\",\"detail\":{\"key\":\"value\"}}";
        Map<String, Object> map = JacksonUtils.parseMap(json);

        assertThat(map.get("detail")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> detail = (Map<String, Object>) map.get("detail");
        assertThat(detail.get("key")).isEqualTo("value");
    }

    /**
     * 测试 toMap 方法正确将对象转换为 Map。
     * <p>
     * 验证对象的属性能被正确转换为 Map 的键值对。
     */
    @Test
    @DisplayName("toMap - 应该正确将对象转换为 Map")
    void toMap_shouldConvertObjectToMap() {
        TestUser user = new TestUser("测试用户", 28);
        Map<String, Object> map = JacksonUtils.toMap(user);

        assertThat(map.get("name")).isEqualTo("测试用户");
        assertThat(map.get("age")).isEqualTo(28);
    }

    /**
     * 测试 toMap 方法忽略 null 值字段。
     * <p>
     * 验证值为 null 的字段不会出现在转换后的 Map 中。
     */
    @Test
    @DisplayName("toMap - null 值字段应该被忽略")
    void toMap_nullValueShouldBeIgnored() {
        TestUser user = new TestUser("测试", null);
        Map<String, Object> map = JacksonUtils.toMap(user);

        assertThat(map.get("name")).isEqualTo("测试");
        assertThat(map).doesNotContainKey("age");
    }

    /**
     * 测试 toObject 方法正确将 Map 转换为对象。
     * <p>
     * 验证 Map 的键值对能被正确转换为对象的属性。
     */
    @Test
    @DisplayName("toObject - 应该正确将 Map 转换为对象")
    void toObject_shouldConvertMapToObject() {
        Map<String, Object> map = Map.of("name", "测试", "age", 30);
        TestUser user = JacksonUtils.toObject(map, TestUser.class);

        assertThat(user.getName()).isEqualTo("测试");
        assertThat(user.getAge()).isEqualTo(30);
    }

    /**
     * 测试 toObject 方法忽略 Map 中的多余字段。
     * <p>
     * 验证 Map 中目标类不存在的字段会被忽略，不影响转换。
     */
    @Test
    @DisplayName("toObject - Map 多余字段应该被忽略")
    void toObject_extraFieldsShouldBeIgnored() {
        Map<String, Object> map = Map.of("name", "测试", "age", 30, "extra", "value");
        TestUser user = JacksonUtils.toObject(map, TestUser.class);

        assertThat(user.getName()).isEqualTo("测试");
        assertThat(user.getAge()).isEqualTo(30);
    }

    /**
     * 测试 deepCopy 方法正确深拷贝对象。
     * <p>
     * 验证深拷贝后的对象与原对象相等但不是同一个实例。
     */
    @Test
    @DisplayName("deepCopy - 应该正确深拷贝对象")
    void deepCopy_shouldDeepCopyObject() {
        TestUser original = new TestUser("原始用户", 25);
        TestUser copy = JacksonUtils.deepCopy(original, TestUser.class);

        assertThat(copy).isEqualTo(original);
        assertThat(copy).isNotSameAs(original);
    }

    /**
     * 测试 deepCopy 方法修改拷贝不影响原对象。
     * <p>
     * 验证深拷贝后的对象修改不会影响原对象的状态。
     */
    @Test
    @DisplayName("deepCopy - 修改拷贝不应影响原对象")
    void deepCopy_modificationShouldNotAffectOriginal() {
        TestUser original = new TestUser("原始用户", 25);
        TestUser copy = JacksonUtils.deepCopy(original, TestUser.class);

        copy.setName("修改后的名字");
        copy.setAge(99);

        assertThat(original.getName()).isEqualTo("原始用户");
        assertThat(original.getAge()).isEqualTo(25);
    }

    /**
     * 测试 setObjectMapper 方法的幂等行为。
     * <p>
     * 验证：相同实例重复设置会被忽略（幂等），不同实例设置会抛出 IllegalStateException。
     */
    @Test
    @DisplayName("setObjectMapper - 幂等设置：相同实例忽略，不同实例抛异常")
    void setObjectMapper_idempotentBehavior() {
        // 保存原始状态
        boolean originalInitialized = false;
        ObjectMapper originalMapper = null;
        try {
            var initializedField = JacksonUtils.class.getDeclaredField("initialized");
            initializedField.setAccessible(true);
            originalInitialized = (boolean) initializedField.get(null);
            originalMapper = JacksonUtils.getObjectMapper();

            // 重置初始化标志，使此测试独立
            initializedField.set(null, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            // 第一次调用成功
            ObjectMapper firstMapper = JacksonMapper.builder().build();
            JacksonUtils.setObjectMapper(firstMapper);
            assertThat(JacksonUtils.getObjectMapper()).isSameAs(firstMapper);

            // 第二次调用相同实例 - 应忽略（幂等）
            JacksonUtils.setObjectMapper(firstMapper);
            assertThat(JacksonUtils.getObjectMapper()).isSameAs(firstMapper);

            // 第三次调用不同实例 - 应抛出 IllegalStateException
            ObjectMapper secondMapper = JacksonMapper.builder().build();
            assertThatThrownBy(() -> JacksonUtils.setObjectMapper(secondMapper))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ObjectMapper has already been initialized with a different instance");
        } finally {
            // 恢复原始状态
            try {
                var initializedField = JacksonUtils.class.getDeclaredField("initialized");
                initializedField.setAccessible(true);
                initializedField.set(null, originalInitialized);
                var mapperField = JacksonUtils.class.getDeclaredField("objectMapper");
                mapperField.setAccessible(true);
                mapperField.set(null, originalMapper);
            } catch (Exception e) {
                // 忽略恢复失败
            }
        }
    }

    /**
     * 测试 LocalDateTime 的完整序列化/反序列化流程。
     * <p>
     * 验证 LocalDateTime 经过序列化和反序列化后能正确还原。
     */
    @Test
    @DisplayName("LocalDateTime 完整序列化/反序列化流程")
    void localDateTime_fullRoundTrip() {
        LocalDateTime original = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
        TestEvent event = new TestEvent("重要会议", original);

        // 序列化
        String json = JacksonUtils.toJson(event);

        // 反序列化
        TestEvent parsed = JacksonUtils.parse(json, TestEvent.class);

        assertThat(parsed.getTitle()).isEqualTo("重要会议");
        assertThat(parsed.getEventTime()).isEqualTo(original);
    }

    /**
     * 测试复杂嵌套对象的序列化和反序列化。
     * <p>
     * 验证包含嵌套对象和数组的复杂 JSON 能被正确解析。
     */
    @Test
    @DisplayName("复杂嵌套对象序列化和反序列化")
    void complexNestedObject_shouldWork() {
        String json = "{\"name\":\"父节点\",\"age\":40,\"children\":[{\"name\":\"子节点1\",\"age\":10}]}";
        Map<String, Object> map = JacksonUtils.parseMap(json);

        assertThat(map.get("name")).isEqualTo("父节点");
        assertThat(map.get("children")).isInstanceOf(List.class);
    }
}
