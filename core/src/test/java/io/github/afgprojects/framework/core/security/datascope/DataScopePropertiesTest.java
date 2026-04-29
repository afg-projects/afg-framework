package io.github.afgprojects.framework.core.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DataScopeProperties 测试
 */
@DisplayName("DataScopeProperties 测试")
class DataScopePropertiesTest {

    @Test
    @DisplayName("应该有默认值")
    void shouldHaveDefaultValues() {
        // when
        DataScopeProperties properties = new DataScopeProperties();

        // then
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getDeptTable()).isEqualTo("sys_dept");
        assertThat(properties.getDeptIdColumn()).isEqualTo("id");
        assertThat(properties.getDeptParentColumn()).isEqualTo("parent_id");
        assertThat(properties.getDefaultScopeType()).isEqualTo(DataScopeType.DEPT);
        assertThat(properties.getUserIdColumn()).isEqualTo("create_by");
        assertThat(properties.isCacheDeptHierarchy()).isTrue();
        assertThat(properties.getCacheExpireSeconds()).isEqualTo(300);
        assertThat(properties.isIgnoreWhenNoContext()).isFalse();
    }
}