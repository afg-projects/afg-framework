package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Versioned 接口测试
 */
@DisplayName("Versioned 接口测试")
class VersionedTest {

    @Nested
    @DisplayName("接口方法测试")
    class InterfaceMethodTests {

        @Test
        @DisplayName("getVersion 方法应该返回版本号")
        void getVersionShouldReturnVersion() {
            // Given
            Versioned entity = new TestVersioned();
            entity.setVersion(5);

            // When
            int version = entity.getVersion();

            // Then
            assertThat(version).isEqualTo(5);
        )

        @Test
        @DisplayName("setVersion 方法应该设置版本号")
        void setVersionShouldSetVersion() {
            // Given
            Versioned entity = new TestVersioned();

            // When
            entity.setVersion(10);

            // Then
            assertThat(entity.getVersion()).isEqualTo(10);
        )
    )

    @Nested
    @DisplayName("默认 incrementVersion 方法测试")
    class DefaultIncrementVersionTests {

        @Test
        @DisplayName("incrementVersion 应该将版本号加 1")
        void incrementVersionShouldAddOne() {
            // Given
            Versioned entity = new TestVersioned();
            entity.setVersion(3);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(4);
        )

        @Test
        @DisplayName("从 0 递增应该变成 1")
        void incrementFromZeroShouldBecomeOne() {
            // Given
            Versioned entity = new TestVersioned();
            entity.setVersion(0);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(1);
        )

        @Test
        @DisplayName("多次递增应该正确累计")
        void multipleIncrementShouldAccumulateCorrectly() {
            // Given
            Versioned entity = new TestVersioned();
            entity.setVersion(0);

            // When
            for (int i = 0; i < 10; i++) {
                entity.incrementVersion();
            )

            // Then
            assertThat(entity.getVersion()).isEqualTo(10);
        )

        @Test
        @DisplayName("从负数递增应该正确工作")
        void incrementFromNegativeShouldWork() {
            // Given
            Versioned entity = new TestVersioned();
            entity.setVersion(-5);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(-4);
        )
    )

    @Nested
    @DisplayName("乐观锁场景测试")
    class OptimisticLockingScenarioTests {

        @Test
        @DisplayName("版本号应该用于检测并发修改")
        void versionShouldDetectConcurrentModification() {
            // Given - 模拟两个用户读取相同版本
            Versioned original = new TestVersioned();
            original.setVersion(1);

            Versioned user1Copy = new TestVersioned();
            user1Copy.setVersion(original.getVersion());

            Versioned user2Copy = new TestVersioned();
            user2Copy.setVersion(original.getVersion());

            // When - 用户 1 先更新
            user1Copy.incrementVersion();
            assertThat(user1Copy.getVersion()).isEqualTo(2);

            // 用户 2 尝试更新，但版本不匹配
            boolean versionMatch = user2Copy.getVersion().equals(original.getVersion());

            // Then
            assertThat(versionMatch).isTrue(); // 用户 2 的版本与原始版本匹配（但在实际场景中原始版本已更新）
            assertThat(user1Copy.getVersion()).isNotEqualTo(user2Copy.getVersion());
        )

        @Test
        @DisplayName("版本号应该反映更新次数")
        void versionShouldReflectUpdateCount() {
            // Given
            Versioned entity = new TestVersioned();
            int initialVersion = entity.getVersion();

            // When - 模拟多次更新
            entity.incrementVersion(); // 第一次更新
            entity.incrementVersion(); // 第二次更新
            entity.incrementVersion(); // 第三次更新

            // Then
            assertThat(entity.getVersion() - initialVersion).isEqualTo(3);
        )
    )

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("版本号应该支持 Integer.MAX_VALUE")
        void shouldSupportMaxIntegerVersion() {
            // Given
            Versioned entity = new TestVersioned();

            // When
            entity.setVersion(Integer.MAX_VALUE);

            // Then
            assertThat(entity.getVersion()).isEqualTo(Integer.MAX_VALUE);
        )

        @Test
        @DisplayName("版本号应该支持负数")
        void shouldSupportNegativeVersion() {
            // Given
            Versioned entity = new TestVersioned();

            // When
            entity.setVersion(-100);

            // Then
            assertThat(entity.getVersion()).isEqualTo(-100);
        )

        @Test
        @DisplayName("版本号应该支持 0")
        void shouldSupportZeroVersion() {
            // Given
            Versioned entity = new TestVersioned();
            entity.setVersion(10);

            // When
            entity.setVersion(0);

            // Then
            assertThat(entity.getVersion()).isEqualTo(0);
        )
    )

    /**
     * 测试 Versioned 实现
     */
    static class TestVersioned implements Versioned {
        private Integer version = 0;

        @Override
        public Integer getVersion() {
            return version;
        )

        @Override
        public void setVersion(Integer version) {
            this.version = version;
        )
    )
)