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
package io.github.afgprojects.framework.data.jdbc.datasource;

import io.github.afgprojects.framework.data.core.annotation.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * 数据源切换切面
 * <p>
 * 拦截标注了 {@link DataSource} 注解的方法或类，在方法执行前切换数据源，
 * 方法执行后自动恢复为之前的数据源。
 *
 * <p>切面优先级设置为 {@link Ordered#HIGHEST_PRECEDENCE}（最高优先级），
 * 确保数据源切换在事务切面之前执行（事务需要使用正确的数据源）。
 *
 * <h3>优先级规则</h3>
 * <ul>
 *   <li>方法级别的 {@code @DataSource} 优先于类级别</li>
 *   <li>编程式切换（{@code DataSourceContextHolder.push()}）优先于注解</li>
 * </ul>
 *
 * @see DataSource
 * @see DataSourceContextHolder
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceAspect {

    /**
     * 环绕通知：拦截 {@link DataSource} 注解
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(io.github.afgprojects.framework.data.core.annotation.DataSource) || " +
            "@within(io.github.afgprojects.framework.data.core.annotation.DataSource)")
    public Object around(@NonNull ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取数据源名称（方法级别优先于类级别）
        String dataSourceName = determineDataSourceName(joinPoint);

        // 如果当前线程已经通过编程式指定了数据源，则不覆盖
        if (DataSourceContextHolder.isSpecified()) {
            log.debug("DataSource already specified via programmatic context: {}, " +
                      "skipping annotation-based switch to: {}",
                      DataSourceContextHolder.peek(), dataSourceName);
            return joinPoint.proceed();
        }

        // 切换数据源
        DataSourceContextHolder.push(dataSourceName);
        log.debug("Switched to datasource: {}", dataSourceName);

        try {
            return joinPoint.proceed();
        } finally {
            // 恢复数据源
            DataSourceContextHolder.pop();
            log.debug("Restored datasource to: {}", DataSourceContextHolder.peek());
        }
    }

    /**
     * 确定数据源名称（方法级别优先于类级别）
     *
     * @param joinPoint 切点
     * @return 数据源名称
     */
    private String determineDataSourceName(@NonNull ProceedingJoinPoint joinPoint) {
        // 优先查找方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataSource methodAnnotation = method.getAnnotation(DataSource.class);
        if (methodAnnotation != null) {
            return methodAnnotation.value();
        }

        // 其次查找类上的注解
        Class<?> targetClass = joinPoint.getTarget().getClass();
        DataSource classAnnotation = targetClass.getAnnotation(DataSource.class);
        if (classAnnotation != null) {
            return classAnnotation.value();
        }

        // 不应该到这里（切面已过滤），返回 null
        return null;
    }
}
