package io.github.afgprojects.framework.core.trace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.Tracer;

/**
 * Baggage 上下文管理
 * <p>
 * 提供分布式追踪 Baggage 的读写能力，支持跨服务传递上下文信息。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>支持标准字段：tenantId、userId、traceId</li>
 *   <li>支持自定义字段</li>
 *   <li>支持远程传播和本地传播</li>
 *   <li>线程安全的读写操作</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 设置 Baggage
 * BaggageContext.set("tenantId", "12345");
 * BaggageContext.set("customField", "value");
 *
 * // 获取 Baggage
 * String tenantId = BaggageContext.get("tenantId");
 *
 * // 获取所有 Baggage
 * Map<String, String> allBaggage = BaggageContext.getAll();
 * }</pre>
 */
public final class BaggageContext {

    /**
     * 标准 Baggage 字段名称
     */
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";
    public static final String TRACE_ID = "traceId";

    /**
     * Baggage 请求头前缀
     */
    public static final String BAGGAGE_HEADER_PREFIX = "X-Baggage-";

    private static final Map<String, String> LOCAL_BAGGAGE = new ConcurrentHashMap<>();

    private BaggageContext() {}

    /**
     * 设置 Baggage 值
     *
     * @param key   字段名称
     * @param value 字段值
     */
    public static void set(String key, @Nullable String value) {
        if (value == null) {
            LOCAL_BAGGAGE.remove(key);
            return;
        }
        LOCAL_BAGGAGE.put(key, value);

        // 如果有 Micrometer Tracer，也设置到 BaggageManager
        Tracer tracer = TraceContext.getTracer();
        if (tracer instanceof BaggageManager baggageManager) {
            var baggage = baggageManager.getBaggage(key);
            if (baggage != null) {
                baggage.set(value);
            } else {
                baggageManager.createBaggage(key, value);
            }
        }
    }

    /**
     * 获取 Baggage 值
     *
     * @param key 字段名称
     * @return 字段值，不存在则返回 null
     */
    public static @Nullable String get(String key) {
        // 优先从 Micrometer Baggage 获取
        Tracer tracer = TraceContext.getTracer();
        if (tracer instanceof BaggageManager baggageManager) {
            var baggage = baggageManager.getBaggage(key);
            if (baggage != null) {
                String value = baggage.get();
                if (value != null) {
                    return value;
                }
            }
        }

        // 回退到本地存储
        return LOCAL_BAGGAGE.get(key);
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID，不存在则返回 null
     */
    public static @Nullable String getTenantId() {
        String tenantId = get(TENANT_ID);
        if (tenantId != null) {
            return tenantId;
        }
        // 回退到 RequestContext
        Long id = AfgRequestContextHolder.getTenantId();
        return id != null ? String.valueOf(id) : null;
    }

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(@Nullable String tenantId) {
        set(TENANT_ID, tenantId);
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID，不存在则返回 null
     */
    public static @Nullable String getUserId() {
        String userId = get(USER_ID);
        if (userId != null) {
            return userId;
        }
        // 回退到 RequestContext
        Long id = AfgRequestContextHolder.getUserId();
        return id != null ? String.valueOf(id) : null;
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(@Nullable String userId) {
        set(USER_ID, userId);
    }

    /**
     * 获取 TraceId
     *
     * @return TraceId，不存在则返回 null
     */
    public static @Nullable String getTraceId() {
        String traceId = get(TRACE_ID);
        if (traceId != null) {
            return traceId;
        }
        // 回退到 TraceContext
        return TraceContext.getTraceId();
    }

    /**
     * 设置 TraceId
     *
     * @param traceId TraceId
     */
    public static void setTraceId(@Nullable String traceId) {
        set(TRACE_ID, traceId);
    }

    /**
     * 获取所有 Baggage
     *
     * @return 所有 Baggage 的副本
     */
    public static Map<String, String> getAll() {
        Map<String, String> result = new ConcurrentHashMap<>();

        // 从本地存储获取
        result.putAll(LOCAL_BAGGAGE);

        // 从 Micrometer Baggage 获取（覆盖本地值）
        Tracer tracer = TraceContext.getTracer();
        if (tracer instanceof BaggageManager baggageManager) {
            Map<String, String> allBaggage = baggageManager.getAllBaggage();
            if (allBaggage != null) {
                result.putAll(allBaggage);
            }
        }

        return result;
    }

    /**
     * 清除所有本地 Baggage
     */
    public static void clear() {
        LOCAL_BAGGAGE.clear();
    }

    /**
     * 判断是否包含指定字段
     *
     * @param key 字段名称
     * @return 是否包含
     */
    public static boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * 移除指定字段
     *
     * @param key 字段名称
     */
    public static void remove(String key) {
        LOCAL_BAGGAGE.remove(key);

        // 同时从 Micrometer Baggage 移除
        Tracer tracer = TraceContext.getTracer();
        if (tracer instanceof BaggageManager baggageManager) {
            var baggage = baggageManager.getBaggage(key);
            if (baggage != null) {
                baggage.set(null);
            }
        }
    }
}
