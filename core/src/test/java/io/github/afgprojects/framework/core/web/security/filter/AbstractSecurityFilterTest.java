package io.github.afgprojects.framework.core.web.security.filter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.model.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class AbstractSecurityFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityChecker checker;

    private TestSecurityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TestSecurityFilter(checker);
    }

    @Nested
    @DisplayName("doFilterInternal tests")
    class DoFilterInternalTests {

        @Test
        @DisplayName("Normal request should pass through filter")
        void shouldPassWhenNoThreat() throws ServletException, IOException {
            when(checker.needsCheck(request)).thenReturn(true);
            when(checker.containsThreat(anyString())).thenReturn(false);
            when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("name")));
            when(request.getParameterValues("name")).thenReturn(new String[] {"test"});

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should throw exception when threat detected")
        void shouldThrowWhenThreatDetected() throws ServletException, IOException {
            when(checker.needsCheck(request)).thenReturn(true);
            when(checker.containsThreat("malicious")).thenReturn(true);
            when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("input")));
            when(request.getParameterValues("input")).thenReturn(new String[] {"malicious"});

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("input");

            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Request not needing check should skip filter")
        void shouldSkipCheckWhenNotNeeded() throws ServletException, IOException {
            when(checker.needsCheck(request)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(checker, never()).containsThreat(anyString());
        }

        @Test
        @DisplayName("Empty parameter values should not cause exception")
        void shouldHandleEmptyParameterValues() throws ServletException, IOException {
            when(checker.needsCheck(request)).thenReturn(true);
            when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("empty")));
            when(request.getParameterValues("empty")).thenReturn(new String[] {});

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(checker, never()).containsThreat(anyString());
        }

        @Test
        @DisplayName("Null parameter values should not cause exception")
        void shouldHandleNullParameterValues() throws ServletException, IOException {
            when(checker.needsCheck(request)).thenReturn(true);
            when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("nullParam")));
            when(request.getParameterValues("nullParam")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(checker, never()).containsThreat(anyString());
        }

        @Test
        @DisplayName("Multiple parameters should all be checked")
        void shouldCheckAllParameters() throws ServletException, IOException {
            when(checker.needsCheck(request)).thenReturn(true);
            when(checker.containsThreat(anyString())).thenReturn(false);
            when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("name", "email", "message")));
            when(request.getParameterValues("name")).thenReturn(new String[] {"John"});
            when(request.getParameterValues("email")).thenReturn(new String[] {"john@example.com"});
            when(request.getParameterValues("message")).thenReturn(new String[] {"Hello"});

            filter.doFilterInternal(request, response, filterChain);

            verify(checker, times(3)).containsThreat(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Multiple values for same parameter should all be checked")
        void shouldCheckAllValuesForParameter() throws ServletException, IOException {
            when(checker.needsCheck(request)).thenReturn(true);
            when(checker.containsThreat(anyString())).thenReturn(false);
            when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("tags")));
            when(request.getParameterValues("tags")).thenReturn(new String[] {"java", "spring", "security"});

            filter.doFilterInternal(request, response, filterChain);

            verify(checker, times(3)).containsThreat(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("No parameters should pass through")
        void shouldPassWhenNoParameters() throws ServletException, IOException {
            when(checker.needsCheck(request)).thenReturn(true);
            when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(checker, never()).containsThreat(anyString());
        }
    }

    /**
     * Test implementation of AbstractSecurityFilter
     */
    private static class TestSecurityFilter extends AbstractSecurityFilter {
        private final SecurityChecker checker;

        TestSecurityFilter(SecurityChecker checker) {
            this.checker = checker;
        }

        @Override
        protected SecurityChecker getChecker() {
            return checker;
        }
    }
}
