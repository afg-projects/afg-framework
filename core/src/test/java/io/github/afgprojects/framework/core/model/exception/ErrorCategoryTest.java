package io.github.afgprojects.framework.core.model.exception;

import io.github.afgprojects.framework.commons.exception.ErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCategory")
class ErrorCategoryTest {

    @Nested
    @DisplayName("prefix")
    class Prefix {

        @Test
        @DisplayName("should return B for BUSINESS")
        void shouldReturnB_forBusiness() {
            assertThat(ErrorCategory.BUSINESS.getPrefix()).isEqualTo("B");
        }

        @Test
        @DisplayName("should return S for SYSTEM")
        void shouldReturnS_forSystem() {
            assertThat(ErrorCategory.SYSTEM.getPrefix()).isEqualTo("S");
        }

        @Test
        @DisplayName("should return N for NETWORK")
        void shouldReturnN_forNetwork() {
            assertThat(ErrorCategory.NETWORK.getPrefix()).isEqualTo("N");
        }

        @Test
        @DisplayName("should return A for SECURITY")
        void shouldReturnA_forSecurity() {
            assertThat(ErrorCategory.SECURITY.getPrefix()).isEqualTo("A");
        }
    }

    @Nested
    @DisplayName("values")
    class Values {

        @Test
        @DisplayName("should have 4 categories")
        void shouldHave4Categories() {
            assertThat(ErrorCategory.values()).hasSize(4);
            assertThat(ErrorCategory.values()).containsExactly(
                    ErrorCategory.BUSINESS, ErrorCategory.SYSTEM,
                    ErrorCategory.NETWORK, ErrorCategory.SECURITY);
        }
    }
}