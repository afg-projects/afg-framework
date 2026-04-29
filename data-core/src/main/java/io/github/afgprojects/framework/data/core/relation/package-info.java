/**
 * 实体关联关系支持
 * <p>
 * 提供实体之间的关联映射注解和关联查询功能。
 * <p>
 * 支持的关联类型：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.data.core.relation.OneToOne} - 一对一关联</li>
 *   <li>{@link io.github.afgprojects.framework.data.core.relation.OneToMany} - 一对多关联</li>
 *   <li>{@link io.github.afgprojects.framework.data.core.relation.ManyToOne} - 多对一关联</li>
 *   <li>{@link io.github.afgprojects.framework.data.core.relation.ManyToMany} - 多对多关联</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * public class User {
 *     @OneToOne
 *     private UserProfile profile;
 *
 *     @OneToMany(mappedBy = "user")
 *     private List<Order> orders;
 *
 *     @ManyToOne
 *     private Department department;
 *
 *     @ManyToMany
 *     private Set<Role> roles;
 * }
 * }</pre>
 *
 * @see io.github.afgprojects.framework.data.core.relation.RelationMetadata
 * @see io.github.afgprojects.framework.data.core.EntityProxy#withAssociation(String)
 */
package io.github.afgprojects.framework.data.core.relation;
