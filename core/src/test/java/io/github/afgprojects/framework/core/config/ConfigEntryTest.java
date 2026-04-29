package io.github.afgprojects.framework.core.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConfigEntryTest {

    @Test
    void should_createEntry_when_usingBuilder() {
        ConfigEntry entry = ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("app.test")
                .value("testValue")
                .build();

        assertNotNull(entry);
    }

    @Test
    void should_haveAllFields_when_entryCreated() {
        long timestamp = System.currentTimeMillis();
        ConfigEntry entry = ConfigEntry.builder()
                .source(ConfigSource.ENVIRONMENT)
                .prefix("app.database.url")
                .value("jdbc:mysql://localhost:3306/mydb")
                .loadedAt(timestamp)
                .build();

        assertEquals(ConfigSource.ENVIRONMENT, entry.source());
        assertEquals("app.database.url", entry.prefix());
        assertEquals("jdbc:mysql://localhost:3306/mydb", entry.value());
        assertEquals(timestamp, entry.loadedAt());
    }

    @Test
    void should_beEqual_when_samePrefixAndSource() {
        ConfigEntry entry1 = ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("app.test")
                .value("value1")
                .loadedAt(1000L)
                .build();

        ConfigEntry entry2 = ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("app.test")
                .value("value2")
                .loadedAt(2000L)
                .build();

        assertEquals(entry1, entry2);
        assertEquals(entry1.hashCode(), entry2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentPrefix() {
        ConfigEntry entry1 = ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("app.test1")
                .value("value")
                .build();

        ConfigEntry entry2 = ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("app.test2")
                .value("value")
                .build();

        assertNotEquals(entry1, entry2);
    }

    @Test
    void should_haveCorrectTimestamp_when_entryCreated() {
        long before = System.currentTimeMillis();
        ConfigEntry entry = ConfigEntry.builder()
                .source(ConfigSource.CONFIG_CENTER)
                .prefix("app.test")
                .value("value")
                .build();
        long after = System.currentTimeMillis();

        assertTrue(entry.loadedAt() >= before);
        assertTrue(entry.loadedAt() <= after);
    }

    @Test
    void should_useProvidedTimestamp_when_loadedAtSpecified() {
        long specificTime = 1234567890L;
        ConfigEntry entry = ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("app.test")
                .value("value")
                .loadedAt(specificTime)
                .build();

        assertEquals(specificTime, entry.loadedAt());
    }

    @Test
    void should_useCurrentTime_when_loadedAtNowCalled() {
        long before = System.currentTimeMillis();
        ConfigEntry entry = ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("app.test")
                .value("value")
                .loadedAtNow()
                .build();
        long after = System.currentTimeMillis();

        assertTrue(entry.loadedAt() >= before);
        assertTrue(entry.loadedAt() <= after);
    }

    @Test
    void should_throwException_when_sourceIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigEntry.builder().prefix("app.test").value("value").build());

        assertEquals("Source cannot be null", exception.getMessage());
    }

    @Test
    void should_throwException_when_prefixIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix(null)
                .value("value")
                .build());

        assertEquals("Prefix cannot be null or blank", exception.getMessage());
    }

    @Test
    void should_throwException_when_prefixIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("   ")
                .value("value")
                .build());

        assertEquals("Prefix cannot be null or blank", exception.getMessage());
    }

    @Test
    void should_haveCorrectToString_when_entryCreated() {
        ConfigEntry entry = ConfigEntry.builder()
                .source(ConfigSource.CURRENT_CONFIG)
                .prefix("app.test")
                .value("value")
                .loadedAt(1000L)
                .build();

        String str = entry.toString();

        assertTrue(str.contains("ConfigEntry"));
        assertTrue(str.contains("CURRENT_CONFIG"));
        assertTrue(str.contains("app.test"));
        assertTrue(str.contains("1000"));
    }
}
