package io.github.afgprojects.framework.core.web.security.filter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.model.exception.BusinessException;

class SqlInjectionFilterTest {

    @Test
    void should_passThrough_when_noSqlInjection() throws Exception {
        SqlInjectionFilter filter = new SqlInjectionFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameterNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void should_throwBusinessException_when_sqlInjectionDetected() throws Exception {
        SqlInjectionFilter filter = new SqlInjectionFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("q")));
        when(request.getParameterValues("q")).thenReturn(new String[] {"' OR 1=1 --"});

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(BusinessException.class);
    }
}
