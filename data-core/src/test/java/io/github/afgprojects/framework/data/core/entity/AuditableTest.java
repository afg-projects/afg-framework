package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Auditable 接口测试
 */
@DisplayName("Auditable 接口测试")
class AuditableTest {

    @Nested
    @DisplayName("标记接口测试")
    class MarkerInterfaceTests {

        @Test
        @DisplayName("Auditable 应该是标记接口（没有方法）")
        void auditableShouldBeMarkerInterface() {
            // Given
            Auditable auditable = new TestAuditableEntity();

            // When & Then - 标记接口，没有方法需要调用
            assertThat(auditable).isInstanceOf(Auditable.class);
            assertThat(Auditable.class.getDeclaredMethods()).isEmpty();
        )

        @Test
        @DisplayName("实体可以同时实现 Auditable 和 AuditableCallback")
        void entityCanImplementBothAuditableAndCallback() {
            // Given
            TestAuditableWithCallback entity = new TestAuditableWithCallback();

            // When & Then
            assertThat(entity).isInstanceOf(Auditable.class);
            assertThat(entity).isInstanceOf(AuditableCallback.class);
        )

        @Test
        @DisplayName("FullEntity 应该实现 Auditable 接口")
        void fullEntityShouldImplementAuditable() {
            // Given
            FullEntity entity = new FullEntity();

            // When & Then
            assertThat(entity).isInstanceOf(Auditable.class);
        )
    )

    @Nested
    @DisplayName("FullEntity 审计字段测试")
    class FullEntityAuditFieldTests {

        @Test
        @DisplayName("FullEntity 应该有 createBy 字段")
        void fullEntityShouldHaveCreateByField() {
            // Given
            FullEntity entity = new FullEntity();

            // When
            entity.setCreateBy("admin");

            // Then
            assertThat(entity.getCreateBy()).isEqualTo("admin");
        )

        @Test
        @DisplayName("FullEntity 应该有 updateBy 字段")
        void fullEntityShouldHaveUpdateByField() {
            // Given
            FullEntity entity = new FullEntity();

            // When
            entity.setUpdateBy("modifier");

            // Then
            assertThat(entity.getUpdateBy()).isEqualTo("modifier");
        )

        @Test
        @DisplayName("审计字段应该支持 null")
        void auditFieldsShouldSupportNull() {
            // Given
            FullEntity entity = new FullEntity();
            entity.setCreateBy("user1");
            entity.setUpdateBy("user2");

            // When
            entity.setCreateBy(null);
            entity.setUpdateBy(null);

            // Then
            assertThat(entity.getCreateBy()).isNull();
            assertThat(entity.getUpdateBy()).isNull();
        )
    )

    /**
     * 测试可审计实体（只实现 Auditable 标记接口）
     */
    static class TestAuditableEntity extends BaseEntity implements Auditable {
        // 只实现标记接口
    )

    /**
     * 测试可审计实体（同时实现 Auditable 和 AuditableCallback）
     */
    static class TestAuditableWithCallback extends BaseEntity implements Auditable, AuditableCallback {

        private String createBy;
        private String updateBy;

        @Override
        public void onCreate(AuditableCallback.AuditContext context) {
            this.createBy = context.getCurrentUser();
            this.setCreatedAt(context.getCurrentTime());
        )

        @Override
        public void onUpdate(AuditableCallback.AuditContext context) {
            this.updateBy = context.getCurrentUser();
            this.setUpdatedAt(context.getCurrentTime());
        )

        public String getCreateBy() {
            return createBy;
        )

        public String getUpdateBy() {
            return updateBy;
        )
    )
)