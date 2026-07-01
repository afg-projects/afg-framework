package io.github.afgprojects.framework.core.autoconfigure;

import java.util.Comparator;
import java.util.List;

import jakarta.servlet.Servlet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.web.security.AfgSecurityConfiguration;
import io.github.afgprojects.framework.core.web.security.AfgSecurityConfigurer;
import io.github.afgprojects.framework.core.web.security.AfgSecurityContextBridge;
import io.github.afgprojects.framework.core.web.security.DefaultAfgSecurityContextBridge;
import io.github.afgprojects.framework.core.web.security.filter.SecurityHeaderFilter;
import io.github.afgprojects.framework.core.web.security.filter.SqlInjectionFilter;
import io.github.afgprojects.framework.core.web.security.filter.XssFilter;
import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;
import io.github.afgprojects.framework.core.web.security.sanitizer.InputSecurityChecker;
import io.github.afgprojects.framework.core.web.security.sanitizer.NoOpInputSanitizer;

/**
 * AFG 安全自动配置
 * <p>
 * 确保始终创建 {@link InputSecurityChecker} bean：
 * <ul>
 *   <li>AntiSamy 在 classpath 上时，创建 {@link EnhancedInputSanitizer}</li>
 *   <li>AntiSamy 不在 classpath 上时，创建 {@link NoOpInputSanitizer}（记录警告，不提供检测）</li>
 * </ul>
 * XssFilter 和 SqlInjectionFilter 始终使用 {@link InputSecurityChecker}，不再依赖正则表达式回退。
 */
@Slf4j
@AutoConfiguration(after = {AfgAutoConfiguration.class, AfgCoreAutoConfiguration.class},
    afterName = {"io.github.afgprojects.framework.security.auth.autoconfigure.CasbinAutoConfiguration"})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Servlet.class)
@ConditionalOnProperty(prefix = "afg.core.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class AfgSecurityAutoConfiguration {

    /**
     * 创建增强输入清洗器（AntiSamy 在 classpath 上时）。
     * <p>
     * 使用 OWASP AntiSamy 进行 XSS 防护，提供比正则表达式更可靠的检测能力。
     */
    @Bean
    @ConditionalOnMissingBean(InputSecurityChecker.class)
    @ConditionalOnClass(name = "org.owasp.validator.html.AntiSamy")
    @ConditionalOnProperty(prefix = "afg.core.security.input-sanitizer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EnhancedInputSanitizer enhancedInputSanitizer(AfgCoreProperties properties) {
        return new EnhancedInputSanitizer(properties);
    }

    /**
     * 创建空操作输入安全检测器（AntiSamy 不在 classpath 上时的降级）。
     * <p>
     * 所有检测方法返回 false，不提供任何安全防护。
     * 记录一条警告日志提示应添加 AntiSamy 依赖。
     */
    @Bean
    @ConditionalOnMissingBean(InputSecurityChecker.class)
    @ConditionalOnProperty(prefix = "afg.core.security.input-sanitizer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public NoOpInputSanitizer noOpInputSanitizer() {
        return new NoOpInputSanitizer();
    }

    /**
     * 创建 XSS 过滤器。
     * <p>
     * 使用 {@link InputSecurityChecker} 进行 XSS 检测。
     * 当 AntiSamy 不在 classpath 上时，检测器为 {@link NoOpInputSanitizer}（不检测）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.core.security.xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    @ConditionalOnBean(InputSecurityChecker.class)
    public XssFilter xssFilter(InputSecurityChecker securityChecker) {
        return new XssFilter(securityChecker);
    }

    /**
     * 创建 SQL 注入过滤器。
     * <p>
     * 使用 {@link InputSecurityChecker} 进行 SQL 注入检测。
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "afg.core.security.sql-injection",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnMissingBean
    @ConditionalOnBean(InputSecurityChecker.class)
    public SqlInjectionFilter sqlInjectionFilter(InputSecurityChecker securityChecker) {
        // SqlInjectionChecker 需要 EnhancedInputSanitizer，但我们可以通过适配处理
        // 对于 SQL 注入检测，EnhancedInputSanitizer 内部仍使用 InputSanitizer.containsSqlInjection()
        // 所以这里直接注入 EnhancedInputSanitizer，如果只有 NoOpInputSanitizer 则跳过
        if (securityChecker instanceof EnhancedInputSanitizer sanitizer) {
            return new SqlInjectionFilter(sanitizer);
        }
        // NoOpInputSanitizer 不支持 SQL 注入检测，使用默认构造函数（正则表达式）
        log.warn("SqlInjectionFilter falling back to regex-based detection because InputSecurityChecker "
                + "is not an EnhancedInputSanitizer. Add AntiSamy dependency for full protection.");
        return new SqlInjectionFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityHeaderFilter securityHeaderFilter(AfgCoreProperties properties) {
        SecurityHeaderFilter filter = new SecurityHeaderFilter();
        AfgCoreProperties.SecurityConfig.SecurityHeaderConfig config = properties.getSecurity().getSecurityHeader();
        if (config.getContentSecurityPolicy() != null) {
            filter.setContentSecurityPolicy(config.getContentSecurityPolicy());
        }
        if (config.getStrictTransportSecurity() != null) {
            filter.setStrictTransportSecurity(config.getStrictTransportSecurity());
        }
        return filter;
    }

    @Bean
    @ConditionalOnMissingBean
    public AfgSecurityContextBridge afgSecurityContextBridge() {
        return new DefaultAfgSecurityContextBridge();
    }

    @Bean
    public AfgSecurityConfiguration afgSecurityConfiguration(List<AfgSecurityConfigurer> configurers) {
        AfgSecurityConfiguration config = new AfgSecurityConfiguration();
        configurers.stream()
                .sorted(Comparator.comparingInt(AfgSecurityConfigurer::getOrder))
                .forEach(c -> c.configure(config));
        return config;
    }
}