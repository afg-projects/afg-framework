package io.github.afgprojects.framework.core.lock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LockType 测试
 */
@DisplayName("LockType 测试")
class LockTypeTest {

    @Test
    @DisplayName("应该包含所有锁类型")
    void shouldContainAllLockTypes() {
        // given
        LockType[] types = LockType.values();

        // then
        assertThat(types).hasSize(4);
        assertThat(types).containsExactlyInAnyOrder(
                LockType.REENTRANT,
                LockType.FAIR,
                LockType.READ,
                LockType.WRITE
        );
    }

    @Test
    @DisplayName("应该正确获取锁类型名称")
    void shouldGetCorrectLockTypeName() {
        assertThat(LockType.REENTRANT.name()).isEqualTo("REENTRANT");
        assertThat(LockType.FAIR.name()).isEqualTo("FAIR");
        assertThat(LockType.READ.name()).isEqualTo("READ");
        assertThat(LockType.WRITE.name()).isEqualTo("WRITE");
    }

    @Test
    @DisplayName("应该正确通过名称获取锁类型")
    void shouldGetLockTypeByName() {
        assertThat(LockType.valueOf("REENTRANT")).isEqualTo(LockType.REENTRANT);
        assertThat(LockType.valueOf("FAIR")).isEqualTo(LockType.FAIR);
        assertThat(LockType.valueOf("READ")).isEqualTo(LockType.READ);
        assertThat(LockType.valueOf("WRITE")).isEqualTo(LockType.WRITE);
    }
}
