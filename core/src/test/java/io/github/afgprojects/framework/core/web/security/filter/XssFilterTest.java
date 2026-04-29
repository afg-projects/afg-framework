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

class XssFilterTest {

    @Test
    void should_passThrough_when_noXssInParams() throws Exception {
        XssFilter filter = new XssFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameterNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void should_throwBusinessException_when_xssDetected() throws Exception {
        XssFilter filter = new XssFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("q")));
        when(request.getParameterValues("q")).thenReturn(new String[] {"<script>alert(1)</script>"});

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(BusinessException.class);
    }
}
