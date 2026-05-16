/**
 * 存储相关 SPI 接口。
 *
 * <p>提供安全相关数据的存储抽象，支持多种存储后端实现。
 *
 * <p>核心接口：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage} - 验证码存储</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.storage.AfgTokenBlacklist} - Token 黑名单</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage} - Refresh Token 存储</li>
 * </ul>
 *
 * <p>实现类可以基于：
 * <ul>
 *   <li>内存存储（适用于单机、开发测试环境）</li>
 *   <li>Redis 存储（适用于分布式、生产环境）</li>
 *   <li>数据库存储（适用于需要持久化的场景）</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.security.core.storage;
