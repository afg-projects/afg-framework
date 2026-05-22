package io.github.afgprojects.framework.security.auth.casbin.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 内存策略服务实现
 *
 * <p>用于测试环境的内存策略存储，不持久化到数据库。
 *
 * @since 1.0.0
 */
public class InMemoryPolicyService implements AfgPolicyService {

    private final List<CasbinRule> policies = new ArrayList<>();

    @Override
    public List<CasbinRule> loadPolicies() {
        return Collections.unmodifiableList(policies);
    }

    @Override
    public void savePolicy(CasbinRule rule) {
        policies.add(rule);
    }

    @Override
    public void removePolicy(CasbinRule rule) {
        policies.remove(rule);
    }

    @Override
    public void clearPolicies() {
        policies.clear();
    }
}
