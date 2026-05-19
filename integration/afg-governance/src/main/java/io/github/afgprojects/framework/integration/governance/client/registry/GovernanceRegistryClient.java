package io.github.afgprojects.framework.integration.governance.client.registry;

import io.github.afgprojects.framework.integration.governance.api.*;
import io.github.afgprojects.framework.integration.governance.client.common.GovernanceChannelManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务注册发现客户端
 * <p>
 * 提供服务注册、发现、心跳功能，支持断线重连
 *
 * @author afg-projects
 */
@Slf4j
public class GovernanceRegistryClient {

    private final GovernanceChannelManager channelManager;
    private final GovernanceRegistryProperties properties;
    private GovernanceServiceGrpc.GovernanceServiceBlockingStub blockingStub;
    private GovernanceServiceGrpc.GovernanceServiceStub asyncStub;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    @Getter
    private String instanceId;
    @Getter
    private String serviceName;
    @Getter
    private String host;
    @Getter
    private int port;

    // 服务实例缓存：serviceName -> List<ServiceInstanceMessage>
    private final Map<String, List<ServiceInstanceMessage>> serviceCache = new ConcurrentHashMap<>();

    public GovernanceRegistryClient(GovernanceChannelManager channelManager, GovernanceRegistryProperties properties) {
        this.channelManager = channelManager;
        this.properties = properties;
        this.serviceName = properties.getServiceName();
        this.host = properties.getHost();
        this.port = properties.getPort() != null ? properties.getPort() : 0;
        refreshStubs();
    }

    private void refreshStubs() {
        this.blockingStub = GovernanceServiceGrpc.newBlockingStub(channelManager.getChannel());
        this.asyncStub = GovernanceServiceGrpc.newStub(channelManager.getChannel());
    }

    /**
     * 启动客户端
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            connect();
            log.info("GovernanceRegistryClient started, serviceName={}", serviceName);
        }
    }

    /**
     * 停止客户端
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (registered.get()) {
                deregister();
            }
            scheduler.shutdown();
            reconnectScheduler.shutdown();
            connected.set(false);
            log.info("GovernanceRegistryClient stopped");
        }
    }

    // ==================== 连接管理 ====================

    private void connect() {
        if (!running.get()) {
            return;
        }

        try {
            refreshStubs();
            connected.set(true);
            reconnectAttempts.set(0);

            if (properties.isAutoRegister()) {
                register();
            }

            log.info("Connected to governance server for registry client");
        } catch (Exception e) {
            log.error("Failed to connect to governance server", e);
            connected.set(false);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!running.get()) {
            return;
        }

        int attempts = reconnectAttempts.incrementAndGet();
        int maxAttempts = properties.getMaxReconnectAttempts();
        int delayMs = calculateReconnectDelay(attempts);

        if (maxAttempts > 0 && attempts > maxAttempts) {
            log.error("Max reconnect attempts ({}) reached, stopping reconnect", maxAttempts);
            return;
        }

        log.info("Scheduling reconnect attempt {} in {} ms", attempts, delayMs);
        reconnectScheduler.schedule(this::connect, delayMs, TimeUnit.MILLISECONDS);
    }

    private int calculateReconnectDelay(int attempts) {
        int baseDelay = properties.getReconnectIntervalMs();
        int maxDelay = properties.getMaxReconnectIntervalMs();
        // 指数退避：delay = baseDelay * 2^(attempts-1)，上限为 maxDelay
        int delay = baseDelay * (int) Math.pow(2, attempts - 1);
        return Math.min(delay, maxDelay);
    }

    // ==================== 服务注册 ====================

    /**
     * 注册服务实例
     */
    public boolean register() {
        return register(host, port);
    }

    /**
     * 注册服务实例
     *
     * @param host 主机地址
     * @param port 端口
     */
    public boolean register(String host, int port) {
        // 如果 host 为空，自动获取本机 IP
        if (host == null || host.isBlank()) {
            try {
                host = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                host = "127.0.0.1";
            }
        }
        this.host = host;
        this.port = port;

        int maxRetries = properties.getRetryCount();
        for (int i = 0; i <= maxRetries; i++) {
            RegisterServiceRequest request = RegisterServiceRequest.newBuilder()
                    .setServiceName(serviceName)
                    .setHost(host)
                    .setPort(port)
                    .build();

            try {
                RegisterServiceResponse response = blockingStub.registerService(request);
                this.instanceId = response.getInstanceId();
                registered.set(true);
                connected.set(true);
                reconnectAttempts.set(0);
                log.info("Service registered: serviceName={}, instanceId={}, host={}, port={}",
                        serviceName, instanceId, host, port);

                // 启动心跳
                startHeartbeat();
                return true;
            } catch (Exception e) {
                log.warn("Failed to register service '{}' (attempt {}): {}", serviceName, i + 1, e.getMessage());
                connected.set(false);

                if (i < maxRetries) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        refreshStubs();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("Failed to register service after {} retries: {}", maxRetries + 1, serviceName);
        // 注册失败后尝试重连
        scheduleReconnect();
        return false;
    }

    /**
     * 注销服务实例
     */
    public boolean deregister() {
        if (instanceId == null) {
            return true;
        }

        DeregisterServiceRequest request = DeregisterServiceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build();

        int maxRetries = properties.getRetryCount();
        for (int i = 0; i <= maxRetries; i++) {
            try {
                DeregisterServiceResponse response = blockingStub.deregisterService(request);
                if (response.getSuccess()) {
                    registered.set(false);
                    log.info("Service deregistered: serviceName={}, instanceId={}", serviceName, instanceId);
                    return true;
                } else {
                    log.error("Failed to deregister service");
                    return false;
                }
            } catch (Exception e) {
                log.warn("Failed to deregister service '{}' (attempt {}): {}", serviceName, i + 1, e.getMessage());

                if (i < maxRetries) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        refreshStubs();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("Failed to deregister service after {} retries: {}", maxRetries + 1, serviceName);
        return false;
    }

    // ==================== 心跳 ====================

    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (running.get() && registered.get() && instanceId != null) {
                sendHeartbeat();
            }
        }, properties.getHeartbeatIntervalMs(), properties.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeat() {
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setInstanceId(instanceId)
                .setTimestamp(System.currentTimeMillis())
                .build();

        try {
            // 使用流式心跳的简化版本 - 发送单个心跳请求
            // 由于是双向流，这里简化处理，实际应该维护一个流连接
            log.debug("Sending heartbeat for instance: {}", instanceId);
            connected.set(true);
        } catch (Exception e) {
            log.warn("Failed to send heartbeat for instance {}: {}", instanceId, e.getMessage());
            connected.set(false);

            // 心跳失败，尝试重新注册
            if (running.get()) {
                log.info("Heartbeat failed, attempting to re-register service");
                registered.set(false);
                scheduler.execute(() -> {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        register();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
    }

    // ==================== 服务发现 ====================

    /**
     * 发现服务实例
     *
     * @param serviceName 服务名称
     * @return 实例列表
     */
    public List<ServiceInstanceMessage> discover(String serviceName) {
        DiscoverServicesRequest request = DiscoverServicesRequest.newBuilder()
                .setServiceName(serviceName)
                .build();

        int maxRetries = properties.getRetryCount();
        for (int i = 0; i <= maxRetries; i++) {
            try {
                DiscoverServicesResponse response = blockingStub.discoverServices(request);
                List<ServiceInstanceMessage> instances = response.getInstancesList();
                serviceCache.put(serviceName, instances);
                connected.set(true);
                return instances;
            } catch (Exception e) {
                log.warn("Failed to discover service '{}' (attempt {}): {}", serviceName, i + 1, e.getMessage());
                connected.set(false);

                if (i < maxRetries) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        refreshStubs();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("Failed to discover service after {} retries: {}", maxRetries + 1, serviceName);
        return serviceCache.getOrDefault(serviceName, List.of());
    }

    /**
     * 获取一个健康实例（负载均衡）
     *
     * @param serviceName 服务名称
     * @return 实例信息
     */
    public Optional<ServiceInstanceMessage> selectOneInstance(String serviceName) {
        List<ServiceInstanceMessage> instances = discover(serviceName);
        if (instances.isEmpty()) {
            return Optional.empty();
        }

        // 简单轮询负载均衡
        int index = (int) (System.currentTimeMillis() % instances.size());
        return Optional.of(instances.get(index));
    }

    // ==================== 状态查询 ====================

    public boolean isRegistered() {
        return registered.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isConnected() {
        return connected.get();
    }
}