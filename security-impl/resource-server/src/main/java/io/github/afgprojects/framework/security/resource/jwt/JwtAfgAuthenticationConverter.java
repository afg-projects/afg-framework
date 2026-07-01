package io.github.afgprojects.framework.security.resource.jwt;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

/**
 * JWT 到 AFG 认证令牌的适配转换器。
 *
 * <p>Spring Security 的 OAuth2 资源服务器期望通过
 * {@code .jwt().jwtAuthenticationConverter(Converter<Jwt, AbstractAuthenticationToken>)} 接入转换器，
 * 但框架的 {@link JwtAuthenticationConverter} 接收的是 {@link JwtAuthenticationToken} 而非 {@link Jwt}，
 * 且其返回的 {@link AfgAuthentication} 未继承 {@link AbstractAuthenticationToken}。
 * 本类提供适配：先将 {@link Jwt} 包装为 {@link JwtAuthenticationToken}，委托
 * {@link JwtAuthenticationConverter} 从 claims 还原 {@link AfgAuthentication}，
 * 再将其 authorities 与用户详情包装回 {@link JwtAuthenticationToken}（一个 {@link AbstractAuthenticationToken} 子类）。
 *
 * <p>转换后 {@code getAuthorities()} 包含从 JWT 的 permissions claim 还原的权限码
 * （三段式 {@code module:resource:action}）以及带 {@code ROLE_} 前缀的角色，供控制器
 * 直接从 {@link org.springframework.security.core.context.SecurityContextHolder} 读取。
 *
 * <p>使用示例：
 * <pre>{@code
 * http.oauth2ResourceServer(oauth2 -> oauth2
 *     .jwt(jwt -> jwt
 *         .jwkSetUri(jwkSetUri)
 *         .jwtAuthenticationConverter(jwtAfgAuthenticationConverter)
 *     )
 * );
 * }</pre>
 *
 * @since 1.1.0
 */
public class JwtAfgAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter delegate;

    /**
     * 使用指定的 {@link JwtAuthenticationConverter} 构造适配器。
     *
     * @param delegate 委托的 JWT 认证转换器（从 claims 还原用户详情与权限）
     */
    public JwtAfgAuthenticationConverter(@NonNull JwtAuthenticationConverter delegate) {
        this.delegate = delegate;
    }

    /**
     * 将 {@link Jwt} 转换为携带 AFG 权限的 {@link AbstractAuthenticationToken}。
     *
     * <p>先用空 authorities 将 {@link Jwt} 包装为 {@link JwtAuthenticationToken} 交给委托转换器，
     * permissions/roles 由委托内部从 claims 重新构造；再用还原出的 authorities 与用户名
     * 重新构建一个 {@link JwtAuthenticationToken} 返回。
     *
     * @param jwt JWT 令牌
     * @return 携带 AFG 权限的认证令牌
     */
    @Override
    @NonNull
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        JwtAuthenticationToken wrapped = new JwtAuthenticationToken(jwt);
        AfgAuthentication afg = delegate.convert(wrapped);
        Collection<? extends GrantedAuthority> authorities = afg.getAuthorities();
        AfgUserDetails userDetails = afg.getUserDetails();
        return new JwtAuthenticationToken(jwt, authorities, userDetails.getUsername());
    }
}

