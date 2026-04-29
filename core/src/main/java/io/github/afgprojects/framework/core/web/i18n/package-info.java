/**
 * 国际化支持包
 * 提供错误消息的国际化支持
 *
 * <p>主要组件：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.i18n.LocaleFilter} - Locale 解析过滤器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.i18n.LocaleAutoConfiguration} - 国际化自动配置</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * // 客户端请求时设置 Accept-Language 头
 * curl -H "Accept-Language: en" http://api.example.com/users
 *
 * // 在代码中获取国际化消息
 * String message = errorCode.getMessage(locale);
 * </pre>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.i18n;
