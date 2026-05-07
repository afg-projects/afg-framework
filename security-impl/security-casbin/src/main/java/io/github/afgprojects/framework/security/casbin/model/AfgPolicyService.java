package io.github.afgprojects.framework.security.casbin.model;

import java.util.List;

/**
 * AFG 策略服务 SPI
 *
 * <p>提供 Casbin 策略的持久化接口，用户需要实现此接口来存储策略数据。
 *
 * <p>支持的存储方式：
 * <ul>
 *   <li>JDBC - 数据库存储</li>
 *   <li>Redis - Redis 存储</li>
 *   <li>Memory - 内存存储（仅用于测试）</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface AfgPolicyService {

    /**
     * 加载所有策略规则
     *
     * @return 策略规则列表
     */
    List<CasbinRule> loadPolicies();

    /**
     * 保存策略规则
     *
     * @param rule 策略规则
     */
    void savePolicy(CasbinRule rule);

    /**
     * 删除策略规则
     *
     * @param rule 策略规则
     */
    void removePolicy(CasbinRule rule);

    /**
     * 清空所有策略规则
     */
    void clearPolicies();
}
