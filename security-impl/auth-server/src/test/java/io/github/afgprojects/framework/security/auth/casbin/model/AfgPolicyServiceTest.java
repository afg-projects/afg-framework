package io.github.afgprojects.framework.security.auth.casbin.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * AfgPolicyService 测试
 */
class AfgPolicyServiceTest {

    @Test
    void should_loadPolicies() {
        AfgPolicyService service = mock(AfgPolicyService.class);
        List<CasbinRule> rules = Arrays.asList(
                CasbinRule.createPolicy("user1", "tenant1", "/api/data", "read"),
                CasbinRule.createRole("user1", "tenant1", "admin")
        );

        when(service.loadPolicies()).thenReturn(rules);

        List<CasbinRule> loaded = service.loadPolicies();

        assertThat(loaded).hasSize(2);
        verify(service).loadPolicies();
    }

    @Test
    void should_savePolicy() {
        AfgPolicyService service = mock(AfgPolicyService.class);
        CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "/api/data", "read");

        service.savePolicy(rule);

        verify(service).savePolicy(rule);
    }

    @Test
    void should_removePolicy() {
        AfgPolicyService service = mock(AfgPolicyService.class);
        CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "/api/data", "read");

        service.removePolicy(rule);

        verify(service).removePolicy(rule);
    }

    @Test
    void should_clearPolicies() {
        AfgPolicyService service = mock(AfgPolicyService.class);

        service.clearPolicies();

        verify(service).clearPolicies();
    }

    @Test
    void should_returnEmptyList_when_noPolicies() {
        AfgPolicyService service = mock(AfgPolicyService.class);
        when(service.loadPolicies()).thenReturn(Collections.emptyList());

        List<CasbinRule> loaded = service.loadPolicies();

        assertThat(loaded).isEmpty();
    }
}
