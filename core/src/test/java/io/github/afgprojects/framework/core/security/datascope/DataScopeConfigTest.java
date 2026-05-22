package io.github.afgprojects.framework.core.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * DataScopeConfig 测试
 */
@DisplayName("DataScopeConfig 测试")
class DataScopeConfigTest {

    @Test
    @DisplayName("应该有默认值")
    void shouldHaveDefaultValues() {
        // when
        AfgCoreProperties.DataScopeConfig config = new AfgCoreProperties.DataScopeConfig();

        // then
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getDeptTable()).isEqualTo("sys_dept");
        assertThat(config.getDeptIdColumn()).isEqualTo("id");
        assertThat(config.getDeptParentColumn()).isEqualTo("parent_id");
        assertThat(config.getDefaultScopeType()).isEqualTo(AfgCoreProperties.DataScopeConfig.DataScopeType.DEPT);
        assertThat(config.getUserIdColumn()).isEqualTo("create_by");
        assertThat(config.isCacheDeptHierarchy()).isTrue();
        assertThat(config.getCacheExpireSeconds()).isEqualTo(300);
        assertThat(config.isIgnoreWhenNoContext()).isFalse();
    }
}