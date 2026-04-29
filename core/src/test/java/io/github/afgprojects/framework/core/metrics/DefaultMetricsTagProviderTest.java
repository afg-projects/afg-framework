package io.github.afgprojects.framework.core.metrics;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultMetricsTagProvider 测试
 */
@DisplayName("DefaultMetricsTagProvider 测试")
class DefaultMetricsTagProviderTest {

    @Test
    @DisplayName("应该提供通用标签")
    void shouldProvideCommonTags() {
        DefaultMetricsTagProvider provider = new DefaultMetricsTagProvider();
        Iterable<Tag> tags = provider.getTags();

        assertNotNull(tags);

        // 验证包含必要的标签
        Iterator<Tag> iterator = tags.iterator();
        assertTrue(iterator.hasNext());

        // 收集所有标签
        java.util.List<Tag> tagList = new java.util.ArrayList<>();
        tags.forEach(tagList::add);

        // 验证有 application, host, version 标签
        assertTrue(tagList.stream().anyMatch(t -> t.getKey().equals("application")));
        assertTrue(tagList.stream().anyMatch(t -> t.getKey().equals("host")));
        assertTrue(tagList.stream().anyMatch(t -> t.getKey().equals("version")));
    }

    @Test
    @DisplayName("application 标签应该有值")
    void applicationTagShouldHaveValue() {
        DefaultMetricsTagProvider provider = new DefaultMetricsTagProvider();
        Iterable<Tag> tags = provider.getTags();

        Tag appTag = null;
        for (Tag tag : tags) {
            if (tag.getKey().equals("application")) {
                appTag = tag;
                break;
            }
        }

        assertNotNull(appTag);
        assertNotNull(appTag.getValue());
        // 值可能是 "unknown" 或者实际的应用名
        assertFalse(appTag.getValue().isEmpty());
    }

    @Test
    @DisplayName("host 标签应该有值")
    void hostTagShouldHaveValue() {
        DefaultMetricsTagProvider provider = new DefaultMetricsTagProvider();
        Iterable<Tag> tags = provider.getTags();

        Tag hostTag = null;
        for (Tag tag : tags) {
            if (tag.getKey().equals("host")) {
                hostTag = tag;
                break;
            }
        }

        assertNotNull(hostTag);
        assertNotNull(hostTag.getValue());
        // 值可能是 "unknown" 或者实际的主机名
        assertFalse(hostTag.getValue().isEmpty());
    }

    @Test
    @DisplayName("version 标签应该有值")
    void versionTagShouldHaveValue() {
        DefaultMetricsTagProvider provider = new DefaultMetricsTagProvider();
        Iterable<Tag> tags = provider.getTags();

        Tag versionTag = null;
        for (Tag tag : tags) {
            if (tag.getKey().equals("version")) {
                versionTag = tag;
                break;
            }
        }

        assertNotNull(versionTag);
        assertNotNull(versionTag.getValue());
        // 值可能是 "unknown" 或者实际的版本号
        assertFalse(versionTag.getValue().isEmpty());
    }

    @Test
    @DisplayName("应该返回 3 个标签")
    void shouldReturnThreeTags() {
        DefaultMetricsTagProvider provider = new DefaultMetricsTagProvider();
        Iterable<Tag> tags = provider.getTags();

        int count = 0;
        for (Tag ignored : tags) {
            count++;
        }

        assertEquals(3, count);
    }
}
