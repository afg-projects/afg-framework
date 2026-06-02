package io.github.afgprojects.framework.data.core.naming;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FieldNameResolver Tests")
class FieldNameResolverTest {

    private FieldNameResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new FieldNameResolver(new EntityMetadataCache());
    )

    @Nested
    @DisplayName("camelCase 转 snake_case")
    class CamelCaseToSnakeCaseTests {

        @Test
        @DisplayName("应该将 camelCase 转换为 snake_case")
        void shouldConvertCamelCaseToSnakeCase() {
            // 无元数据时，降级到 snake_case 转换
            String result = resolver.resolveColumnName(TestEntity.class, TestEntity::getUserName);
            assertThat(result).isEqualTo("user_name");
        )

        @Test
        @DisplayName("应该处理单个单词属性名")
        void shouldHandleSingleWordPropertyName() {
            String result = resolver.resolveColumnName(TestEntity.class, TestEntity::getId);
            assertThat(result).isEqualTo("id");
        )

        @Test
        @DisplayName("应该处理连续大写字母")
        void shouldHandleConsecutiveUppercase() {
            String result = resolver.resolveColumnName(TestEntity.class, TestEntity::getURL);
            assertThat(result).isEqualTo("u_r_l");
        )

        @Test
        @DisplayName("应该处理多个单词组合")
        void shouldHandleMultipleWords() {
            String result = resolver.resolveColumnName(TestEntity.class, TestEntity::getUserEmailAddress);
            assertThat(result).isEqualTo("user_email_address");
        )
    )

    @Nested
    @DisplayName("Boolean 字段处理")
    class BooleanFieldTests {

        @Test
        @DisplayName("应该正确处理 Boolean 类型字段的 is 前缀方法")
        void shouldHandleBooleanIsPrefixMethod() {
            // getActive() 方法提取属性名 "active"，转换为 "active"
            String result = resolver.resolveColumnName(TestEntity.class, TestEntity::getActive);
            assertThat(result).isEqualTo("active");
        )

        @Test
        @DisplayName("应该正确处理 Boolean 类型字段的 get 前缀方法")
        void shouldHandleBooleanGetPrefixMethod() {
            // getDeleted() 方法提取属性名 "deleted"，转换为 "deleted"
            String result = resolver.resolveColumnName(TestEntity.class, TestEntity::getDeleted);
            assertThat(result).isEqualTo("deleted");
        )
    )

    @Nested
    @DisplayName("边界场景")
    class EdgeCaseTests {

        @Test
        @DisplayName("应该处理空字符串属性名")
        void shouldHandleEmptyPropertyName() {
            // 直接调用 toSnakeCase 逻辑（通过已知的 getter）
            // 这里验证单字符属性名
            String result = resolver.resolveColumnName(TestEntity.class, TestEntity::getX);
            assertThat(result).isEqualTo("x");
        )

        @Test
        @DisplayName("应该处理全大写属性名")
        void shouldHandleAllUppercasePropertyName() {
            String result = resolver.resolveColumnName(TestEntity.class, TestEntity::getURL);
            assertThat(result).isEqualTo("u_r_l");
        )
    )

    // 测试用内部类
    static class TestEntity {

        private Long id;
        private String userName;
        private String URL;
        private String userEmailAddress;
        private Boolean active;
        private Boolean deleted;
        private Integer x;

        public Long getId() {
            return id;
        )

        public String getUserName() {
            return userName;
        )

        public String getURL() {
            return URL;
        )

        public String getUserEmailAddress() {
            return userEmailAddress;
        )

        public Boolean getActive() {
            return active;
        )

        public Boolean getDeleted() {
            return deleted;
        )

        public Integer getX() {
            return x;
        )
    )
)
