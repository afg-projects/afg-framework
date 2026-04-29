package io.github.afgprojects.framework.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

class ConfigSourceTest extends BaseUnitTest {

    @Test
    void should_haveCorrectPriority_when_enumCreated() {
        assertThat(ConfigSource.MODULE_DEFAULT.getPriority()).isEqualTo(0);
        assertThat(ConfigSource.DEPENDENCY_CONFIG.getPriority()).isEqualTo(1);
        assertThat(ConfigSource.CURRENT_CONFIG.getPriority()).isEqualTo(2);
        assertThat(ConfigSource.ENVIRONMENT.getPriority()).isEqualTo(3);
        assertThat(ConfigSource.COMMAND_LINE.getPriority()).isEqualTo(4);
        assertThat(ConfigSource.CONFIG_CENTER.getPriority()).isEqualTo(5);
    }

    @Test
    void should_returnTrue_when_configCenterSupportsRefresh() {
        assertThat(ConfigSource.CONFIG_CENTER.supportsRefresh()).isTrue();
    }

    @Test
    void should_returnFalse_when_otherSourceSupportsRefresh() {
        assertThat(ConfigSource.MODULE_DEFAULT.supportsRefresh()).isFalse();
        assertThat(ConfigSource.DEPENDENCY_CONFIG.supportsRefresh()).isFalse();
        assertThat(ConfigSource.CURRENT_CONFIG.supportsRefresh()).isFalse();
        assertThat(ConfigSource.ENVIRONMENT.supportsRefresh()).isFalse();
        assertThat(ConfigSource.COMMAND_LINE.supportsRefresh()).isFalse();
    }

    @Test
    void should_orderByPriority_when_sorted() {
        List<ConfigSource> sources =
                Arrays.asList(ConfigSource.CONFIG_CENTER, ConfigSource.MODULE_DEFAULT, ConfigSource.ENVIRONMENT);

        sources.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        assertThat(sources)
                .containsExactly(ConfigSource.CONFIG_CENTER, ConfigSource.ENVIRONMENT, ConfigSource.MODULE_DEFAULT);
    }
}
