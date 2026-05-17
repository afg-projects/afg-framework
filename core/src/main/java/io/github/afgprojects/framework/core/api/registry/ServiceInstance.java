package io.github.afgprojects.framework.core.api.registry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 服务实例模型。
 *
 * <p>表示一个服务实例的完整信息，包括网络位置、元数据、状态等。
 *
 * <p>使用示例：
 * <pre>{@code
 * ServiceInstance instance = ServiceInstance.builder()
 *     .serviceId("user-service")
 *     .host("192.168.1.100")
 *     .port(8080)
 *     .metadata("version", "1.0.0")
 *     .build();
 *
 * URI uri = instance.getUri();  // http://192.168.1.100:8080
 * }</pre>
 *
 * @since 1.0.0
 */
public class ServiceInstance {

    private final String serviceId;
    private final String instanceId;
    private final String host;
    private final int port;
    private final boolean secure;
    private final Map<String, String> metadata;
    private final Status status;
    private final int weight;
    private final long registrationTime;

    private ServiceInstance(Builder builder) {
        this.serviceId = Objects.requireNonNull(builder.serviceId, "serviceId must not be null");
        this.instanceId = builder.instanceId != null ? builder.instanceId : generateInstanceId();
        this.host = Objects.requireNonNull(builder.host, "host must not be null");
        this.port = builder.port;
        this.secure = builder.secure;
        this.metadata = Collections.unmodifiableMap(
            builder.metadata != null ? builder.metadata : new HashMap<>());
        this.status = builder.status != null ? builder.status : Status.UP;
        this.weight = builder.weight > 0 ? builder.weight : 1;
        this.registrationTime = builder.registrationTime > 0
            ? builder.registrationTime : System.currentTimeMillis();
    }

    /**
     * 获取服务 ID。
     *
     * @return 服务 ID
     */
    @NonNull
    public String getServiceId() {
        return serviceId;
    }

    /**
     * 获取实例 ID。
     *
     * @return 实例 ID
     */
    @NonNull
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 获取主机地址。
     *
     * @return 主机地址
     */
    @NonNull
    public String getHost() {
        return host;
    }

    /**
     * 获取端口。
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 是否使用 HTTPS。
     *
     * @return 如果使用 HTTPS 返回 true
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * 获取服务 URI。
     *
     * @return 服务 URI（http(s)://host:port）
     */
    @NonNull
    public URI getUri() {
        String scheme = secure ? "https" : "http";
        return URI.create(scheme + "://" + host + ":" + port);
    }

    /**
     * 获取元数据。
     *
     * @return 元数据 Map
     */
    @NonNull
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * 获取指定元数据。
     *
     * @param key 元数据键
     * @return 元数据值，不存在返回 null
     */
    @Nullable
    public String getMetadata(@NonNull String key) {
        return metadata.get(key);
    }

    /**
     * 获取实例状态。
     *
     * @return 实例状态
     */
    @NonNull
    public Status getStatus() {
        return status;
    }

    /**
     * 获取实例权重。
     *
     * @return 权重值（用于负载均衡）
     */
    public int getWeight() {
        return weight;
    }

    /**
     * 获取注册时间。
     *
     * @return 注册时间戳（毫秒）
     */
    public long getRegistrationTime() {
        return registrationTime;
    }

    private String generateInstanceId() {
        return serviceId + "-" + host + "-" + port + "-" + System.currentTimeMillis();
    }

    /**
     * 创建 Builder。
     *
     * @return Builder 实例
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
            "serviceId='" + serviceId + '\'' +
            ", instanceId='" + instanceId + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port +
            ", secure=" + secure +
            ", status=" + status +
            ", weight=" + weight +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return Objects.equals(instanceId, that.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId);
    }

    /**
     * 实例状态枚举。
     */
    public enum Status {
        /**
         * 正常运行。
         */
        UP,

        /**
         * 下线。
         */
        DOWN,

        /**
         * 启动中。
         */
        STARTING,

        /**
         * 停止中。
         */
        STOPPING,

        /**
         * 不健康（暂时不提供服务）。
         */
        OUT_OF_SERVICE
    }

    /**
     * Builder 类。
     */
    public static class Builder {
        private String serviceId;
        private String instanceId;
        private String host;
        private int port;
        private boolean secure;
        private Map<String, String> metadata = new HashMap<>();
        private Status status;
        private int weight;
        private long registrationTime;

        /**
         * 设置服务 ID。
         *
         * @param serviceId 服务 ID
         * @return this
         */
        public @NonNull Builder serviceId(@NonNull String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        /**
         * 设置实例 ID。
         *
         * @param instanceId 实例 ID
         * @return this
         */
        public @NonNull Builder instanceId(@Nullable String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * 设置主机地址。
         *
         * @param host 主机地址
         * @return this
         */
        public @NonNull Builder host(@NonNull String host) {
            this.host = host;
            return this;
        }

        /**
         * 设置端口。
         *
         * @param port 端口号
         * @return this
         */
        public @NonNull Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * 设置是否使用 HTTPS。
         *
         * @param secure 是否使用 HTTPS
         * @return this
         */
        public @NonNull Builder secure(boolean secure) {
            this.secure = secure;
            return this;
        }

        /**
         * 设置元数据。
         *
         * @param metadata 元数据 Map
         * @return this
         */
        public @NonNull Builder metadata(@NonNull Map<String, String> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        /**
         * 添加单个元数据。
         *
         * @param key   元数据键
         * @param value 元数据值
         * @return this
         */
        public @NonNull Builder metadata(@NonNull String key, @NonNull String value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * 设置实例状态。
         *
         * @param status 实例状态
         * @return this
         */
        public @NonNull Builder status(@NonNull Status status) {
            this.status = status;
            return this;
        }

        /**
         * 设置实例权重。
         *
         * @param weight 权重值
         * @return this
         */
        public @NonNull Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        /**
         * 设置注册时间。
         *
         * @param registrationTime 注册时间戳
         * @return this
         */
        public @NonNull Builder registrationTime(long registrationTime) {
            this.registrationTime = registrationTime;
            return this;
        }

        /**
         * 构建 ServiceInstance。
         *
         * @return ServiceInstance 实例
         */
        public @NonNull ServiceInstance build() {
            return new ServiceInstance(this);
        }
    }
}
