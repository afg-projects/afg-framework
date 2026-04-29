/**
 * 配置管理核心包。
 *
 * <p>提供统一的配置管理能力，支持多配置源优先级、配置刷新和变更监听。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.config.AfgConfigRegistry} - 配置注册中心，线程安全</li>
 *   <li>{@link io.github.afgprojects.framework.core.config.ConfigSource} - 配置源枚举（优先级从低到高）</li>
 *   <li>{@link io.github.afgprojects.framework.core.config.ConfigEntry} - 配置条目数据类</li>
 *   <li>{@link io.github.afgprojects.framework.core.config.ConfigRefresher} - 配置刷新器</li>
 *   <li>{@link io.github.afgprojects.framework.core.config.ConfigChangeListener} - 配置变更监听接口</li>
 * </ul>
 *
 * <p>配置源优先级（从低到高）：
 * <ol>
 *   <li>MODULE_DEFAULT - 模块默认配置</li>
 *   <li>DEPENDENCY_CONFIG - 依赖模块配置</li>
 *   <li>CURRENT_CONFIG - 当前配置</li>
 *   <li>ENVIRONMENT - 环境变量</li>
 *   <li>COMMAND_LINE - 命令行参数</li>
 *   <li>CONFIG_CENTER - 配置中心</li>
 * </ol>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.config;
