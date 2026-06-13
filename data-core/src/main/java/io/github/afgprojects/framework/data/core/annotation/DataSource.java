/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据源切换注解
 * <p>
 * 标注在方法或类上，指定该方法或类使用的数据源名称。
 * 配合 {@code DataSourceAspect} 切面在方法执行前切换数据源，
 * 方法执行后自动恢复为默认数据源。
 *
 * <h3>使用示例</h3>
 * <pre>
 * &#064;Service
 * public class OrderService {
 *
 *     // 使用 slave_1 数据源查询
 *     &#064;DataSource("slave_1")
 *     public List&lt;Order&gt; queryOrders() {
 *         return dataManager.findAll(Order.class);
 *     }
 *
 *     // 使用默认数据源写入
 *     public Order createOrder(Order order) {
 *         return dataManager.save(Order.class, order);
 *     }
 * }
 * </pre>
 *
 * <h3>类级别注解</h3>
 * <p>
 * 标注在类上时，该类所有方法都使用指定数据源。
 * 方法级别的 {@code @DataSource} 会覆盖类级别的注解。
 *
 * <h3>与 withDataSource() 的关系</h3>
 * <p>
 * {@code @DataSource} 注解适用于声明式数据源切换（AOP 切面自动处理），
 * 而 {@code dataManager.entity(Xxx.class).query().withDataSource(name)} 适用于
 * 编程式数据源切换。两者可以配合使用，编程式优先级更高。
 *
 * @see io.github.afgprojects.framework.data.core.EntityQuery#withDataSource(String)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSource {

    /**
     * 数据源名称
     * <p>
     * 对应配置中定义的数据源名称（如 "slave_1"、"master" 等）。
     *
     * @return 数据源名称
     */
    String value();
}
