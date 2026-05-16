package io.github.afgprojects.framework.security.core.login.strategy;

import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 登录策略工厂。
 *
 * <p>管理和查找登录策略实现。
 *
 * <p>支持通过 Spring 自动注入所有 {@link LoginStrategy} 实现。
 *
 * @since 1.0.0
 */
public class LoginStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(LoginStrategyFactory.class);

    private final Map<String, LoginStrategy> strategyMap = new HashMap<>();

    /**
     * 注册登录策略。
     *
     * @param strategy 登录策略
     */
    public void register(@NonNull LoginStrategy strategy) {
        String loginType = strategy.getLoginType();
        if (strategyMap.containsKey(loginType)) {
            log.warn("Login strategy '{}' already registered, will be overwritten", loginType);
        }
        strategyMap.put(loginType, strategy);
        log.debug("Registered login strategy: {}", loginType);
    }

    /**
     * 批量注册登录策略。
     *
     * @param strategies 登录策略集合
     */
    public void registerAll(@Nullable Collection<LoginStrategy> strategies) {
        if (strategies != null) {
            strategies.forEach(this::register);
        }
    }

    /**
     * 根据登录类型获取策略。
     *
     * @param loginType 登录类型
     * @return 登录策略，如果不存在返回 empty
     */
    @NonNull
    public Optional<LoginStrategy> getStrategy(@NonNull String loginType) {
        return Optional.ofNullable(strategyMap.get(loginType));
    }

    /**
     * 根据登录请求获取策略。
     *
     * <p>优先使用 {@link LoginStrategy#supports(LoginRequest)} 方法匹配。
     *
     * @param request 登录请求
     * @return 登录策略，如果不存在返回 empty
     */
    @NonNull
    public Optional<LoginStrategy> getStrategy(@NonNull LoginRequest request) {
        // 优先使用 supports 方法匹配
        return strategyMap.values().stream()
                .filter(strategy -> strategy.supports(request))
                .findFirst()
                .or(() -> getStrategy(request.loginType().name()));
    }

    /**
     * 获取所有已注册的登录类型。
     *
     * @return 登录类型集合
     */
    @NonNull
    public Collection<String> getRegisteredTypes() {
        return strategyMap.keySet();
    }

    /**
     * 检查登录类型是否已注册。
     *
     * @param loginType 登录类型
     * @return 是否已注册
     */
    public boolean isRegistered(@NonNull String loginType) {
        return strategyMap.containsKey(loginType);
    }
}