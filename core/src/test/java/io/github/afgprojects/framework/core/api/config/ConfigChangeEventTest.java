package io.github.afgprojects.framework.core.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ConfigChangeEvent} 配置变更事件测试
 *
 * <p>测试配置变更事件的创建和类型判断：
 * <ul>
 *   <li>ADDED - 配置新增</li>
 *   <li>MODIFIED - 配置修改</li>
 *   <li>DELETED - 配置删除</li>
 * </ul>
 *
 * @see ConfigChangeEvent
 */
@DisplayName("ConfigChangeEvent 测试")
class ConfigChangeEventTest {

    /**
     * 测试创建 ADDED 类型的事件
     */
    @Test
    @DisplayName("应该创建 ADDED 类型的配置变更事件")
    void shouldCreateAddedEvent() {
        ConfigChangeEvent event = new ConfigChangeEvent(
                "db.url",
                "database",
                null,
                "jdbc:mysql://localhost:3306/mydb",
                ConfigChangeEvent.ConfigChangeType.ADDED
        );

        assertEquals("db.url", event.key());
        assertEquals("database", event.group());
        assertNull(event.oldValue());
        assertEquals("jdbc:mysql://localhost:3306/mydb", event.newValue());
        assertEquals(ConfigChangeEvent.ConfigChangeType.ADDED, event.changeType());
        assertTrue(event.isAddition());
        assertFalse(event.isModification());
        assertFalse(event.isDeletion());
    }

    /**
     * 测试创建 MODIFIED 类型的事件
     */
    @Test
    @DisplayName("应该创建 MODIFIED 类型的配置变更事件")
    void shouldCreateModifiedEvent() {
        ConfigChangeEvent event = new ConfigChangeEvent(
                "db.pool.size",
                "database",
                "10",
                "20",
                ConfigChangeEvent.ConfigChangeType.MODIFIED
        );

        assertEquals("db.pool.size", event.key());
        assertEquals("database", event.group());
        assertEquals("10", event.oldValue());
        assertEquals("20", event.newValue());
        assertEquals(ConfigChangeEvent.ConfigChangeType.MODIFIED, event.changeType());
        assertFalse(event.isAddition());
        assertTrue(event.isModification());
        assertFalse(event.isDeletion());
    }

    /**
     * 测试创建 DELETED 类型的事件
     */
    @Test
    @DisplayName("应该创建 DELETED 类型的配置变更事件")
    void shouldCreateDeletedEvent() {
        ConfigChangeEvent event = new ConfigChangeEvent(
                "cache.enabled",
                "cache",
                "true",
                null,
                ConfigChangeEvent.ConfigChangeType.DELETED
        );

        assertEquals("cache.enabled", event.key());
        assertEquals("cache", event.group());
        assertEquals("true", event.oldValue());
        assertNull(event.newValue());
        assertEquals(ConfigChangeEvent.ConfigChangeType.DELETED, event.changeType());
        assertFalse(event.isAddition());
        assertFalse(event.isModification());
        assertTrue(event.isDeletion());
    }

    /**
     * 测试 ConfigChangeType 枚举包含所有类型
     */
    @Test
    @DisplayName("ConfigChangeType 枚举应该包含所有类型")
    void configChangeTypeShouldContainAllTypes() {
        ConfigChangeEvent.ConfigChangeType[] types = ConfigChangeEvent.ConfigChangeType.values();
        assertEquals(3, types.length);
        assertEquals(ConfigChangeEvent.ConfigChangeType.ADDED, ConfigChangeEvent.ConfigChangeType.valueOf("ADDED"));
        assertEquals(ConfigChangeEvent.ConfigChangeType.MODIFIED, ConfigChangeEvent.ConfigChangeType.valueOf("MODIFIED"));
        assertEquals(ConfigChangeEvent.ConfigChangeType.DELETED, ConfigChangeEvent.ConfigChangeType.valueOf("DELETED"));
    }
}
