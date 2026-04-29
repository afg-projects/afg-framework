package io.github.afgprojects.framework.core.model.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorCategoryTest {

    @Test
    @DisplayName("应返回正确的前缀")
    void shouldReturnCorrectPrefix() {
        assertThat(ErrorCategory.BUSINESS.getPrefix()).isEqualTo("B");
        assertThat(ErrorCategory.SYSTEM.getPrefix()).isEqualTo("S");
        assertThat(ErrorCategory.NETWORK.getPrefix()).isEqualTo("N");
        assertThat(ErrorCategory.SECURITY.getPrefix()).isEqualTo("A");
    }

    @Test
    @DisplayName("应包含所有分类")
    void shouldContainAllCategories() {
        assertThat(ErrorCategory.values())
                .containsExactlyInAnyOrder(
                        ErrorCategory.BUSINESS, ErrorCategory.SYSTEM, ErrorCategory.NETWORK, ErrorCategory.SECURITY);
    }
}
