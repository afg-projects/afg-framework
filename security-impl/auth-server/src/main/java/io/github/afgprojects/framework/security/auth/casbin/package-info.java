/**
 * AFG Framework Security Casbin 模块
 *
 * <p>此模块提供 Casbin 权限控制的集成实现，包括：
 * <ul>
 *   <li>CasbinAfgEnforcer - 实现 AfgEnforcer 接口的权限执行器</li>
 *   <li>CasbinProperties - Casbin 配置属性</li>
 *   <li>AfgPolicyService - 策略服务 SPI</li>
 * </ul>
 *
 * <p>支持 RBAC with domains 模型：
 * <ul>
 *   <li>sub - 主体（用户/角色）</li>
 *   <li>dom - 域（租户）</li>
 *   <li>obj - 对象（资源）</li>
 *   <li>act - 动作（操作）</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.security.auth.casbin;
