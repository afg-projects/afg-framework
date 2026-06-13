package io.github.afgprojects.framework.security.core.authentication;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * AFG 用户服务 SPI。
 *
 * <p>定义加载用户详情的标准接口，业务系统需要实现此接口以提供用户信息。
 *
 * <p>实现类应从数据库、LDAP 或其他用户存储加载用户信息，并封装为 {@link AfgUserDetails}。
 *
 * @since 1.0.0
 */
public interface AfgUserDetailsService {

    /**
     * 根据用户名加载用户详情。
     *
     * @param username 用户名（登录名），永不为 null
     * @return 用户详情，永不为 null
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @NonNull
    AfgUserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException;

    /**
     * 根据用户 ID 加载用户详情。
     *
     * @param userId 用户 ID，永不为 null
     * @return 用户详情，永不为 null
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @NonNull
    AfgUserDetails loadUserByUserId(@NonNull String userId) throws UsernameNotFoundException;

    /**
     * 根据用户名和租户 ID 加载用户详情。
     *
     * <p>多租户场景下，同一用户名可能在不同租户中存在。
     *
     * @param username 用户名（登录名），永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 用户详情，永不为 null
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @NonNull
    default AfgUserDetails loadUserByUsernameAndTenant(
            @NonNull String username, @Nullable String tenantId) throws UsernameNotFoundException {
        // 默认实现：忽略租户 ID，调用标准方法
        return loadUserByUsername(username);
    }

    /**
     * 根据手机号加载用户详情。
     *
     * @param mobile 手机号，永不为 null
     * @return 用户详情，永不为 null
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @NonNull
    default AfgUserDetails loadUserByMobile(@NonNull String mobile) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Mobile login not supported");
    }

    /**
     * 根据邮箱加载用户详情。
     *
     * @param email 邮箱，永不为 null
     * @return 用户详情，永不为 null
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @NonNull
    default AfgUserDetails loadUserByEmail(@NonNull String email) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Email login not supported");
    }

    /**
     * 根据社交平台 openId 加载用户详情。
     *
     * <p>社交登录场景下，通过第三方平台返回的用户唯一标识查找对应的系统用户。
     * 业务系统需要实现此方法以支持社交登录策略的用户映射。
     *
     * @param openId 第三方平台用户唯一标识，永不为 null
     * @param source 来源平台标识（如 wechat、dingtalk、feishu、wecom），永不为 null
     * @return 用户详情，永不为 null
     * @throws UsernameNotFoundException 如果用户不存在或未绑定系统账号
     */
    @NonNull
    default AfgUserDetails loadUserBySocialOpenId(@NonNull String openId, @NonNull String source) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Social login not supported");
    }
}
