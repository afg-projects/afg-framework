package io.github.afgprojects.framework.core.web.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AfgRequestContextHolderTest {

    @BeforeEach
    void setUp() {
        // Setup Spring RequestContextHolder with mock request
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void should_setAndGetContext_when_contextMethodsCalled() {
        // Given
        RequestContext context = RequestContext.builder()
                .traceId("trace-123")
                .requestId("request-456")
                .userId(1L)
                .build();

        // When
        AfgRequestContextHolder.setContext(context);
        RequestContext retrieved = AfgRequestContextHolder.getContext();

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getTraceId()).isEqualTo("trace-123");
        assertThat(retrieved.getRequestId()).isEqualTo("request-456");
        assertThat(retrieved.getUserId()).isEqualTo(1L);
    }

    @Test
    void should_returnNull_when_contextNotSet() {
        // When
        RequestContext context = AfgRequestContextHolder.getContext();

        // Then
        assertThat(context).isNull();
    }

    @Test
    void should_clearContext_when_clearCalled() {
        // Given
        RequestContext context = new RequestContext();
        AfgRequestContextHolder.setContext(context);

        // When
        AfgRequestContextHolder.clear();

        // Then
        assertThat(AfgRequestContextHolder.getContext()).isNull();
    }

    @Test
    void should_returnTraceId_when_getTraceIdCalled() {
        // Given
        RequestContext context = RequestContext.builder().traceId("trace-abc").build();
        AfgRequestContextHolder.setContext(context);

        // When
        String traceId = AfgRequestContextHolder.getTraceId();

        // Then
        assertThat(traceId).isEqualTo("trace-abc");
    }

    @Test
    void should_returnNullTraceId_when_contextNotSet() {
        // When
        String traceId = AfgRequestContextHolder.getTraceId();

        // Then
        assertThat(traceId).isNull();
    }

    @Test
    void should_returnUserId_when_getUserIdCalled() {
        // Given
        RequestContext context = RequestContext.builder().userId(42L).build();
        AfgRequestContextHolder.setContext(context);

        // When
        Long userId = AfgRequestContextHolder.getUserId();

        // Then
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void should_returnTenantId_when_getTenantIdCalled() {
        // Given
        RequestContext context = RequestContext.builder().tenantId(100L).build();
        AfgRequestContextHolder.setContext(context);

        // When
        Long tenantId = AfgRequestContextHolder.getTenantId();

        // Then
        assertThat(tenantId).isEqualTo(100L);
    }

    @Test
    void should_returnNull_when_noRequestAttributes() {
        // Given
        RequestContextHolder.resetRequestAttributes();

        // When
        RequestContext context = AfgRequestContextHolder.getContext();

        // Then
        assertThat(context).isNull();
    }
}
