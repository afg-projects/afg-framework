package io.github.afgprojects.framework.governance.server.grpc;

import io.github.afgprojects.framework.governance.proto.*;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigGroup;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.service.ServiceInstance;
import io.github.afgprojects.framework.governance.server.service.config.ConfigGroupService;
import io.github.afgprojects.framework.governance.server.service.config.ConfigItemService;
import io.github.afgprojects.framework.governance.server.service.config.ConfigValueService;
import io.github.afgprojects.framework.governance.server.service.registry.ServiceRegistryService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * gRPC service implementation for governance configuration management.
 * Provides configuration subscription, retrieval, and publishing capabilities.
 * Delegates data operations to the Service layer.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GovernanceGrpcServiceImpl extends GovernanceServiceGrpc.GovernanceServiceImplBase {

    private final ConfigStreamManager streamManager;
    private final ConfigItemService itemService;
    private final ConfigValueService valueService;
    private final ConfigGroupService groupService;
    private final ServiceRegistryService registryService;
    private final ServiceHeartbeatManager heartbeatManager;

    /**
     * Subscribe to configuration changes.
     * This is a server-side streaming RPC that keeps the connection open
     * and pushes configuration changes to the client.
     */
    @Override
    public void subscribeConfig(SubscribeConfigRequest request,
                                StreamObserver<ConfigChangeNotification> responseObserver) {
        String serviceName = request.getServiceName();
        log.info("Config subscription request from service: {}", serviceName);

        // Register the stream
        streamManager.registerStream(serviceName, responseObserver);

        // Push current configurations immediately
        Map<String, String> configs = valueService.getAllValues();
        configs.forEach((key, value) -> {
            ConfigChangeNotification notification = ConfigChangeNotification.newBuilder()
                .setKey(key)
                .setValue(value)
                .setChangeType(ChangeType.CHANGE_TYPE_CREATE)
                .setTimestamp(System.currentTimeMillis())
                .build();
            responseObserver.onNext(notification);
        });

        // Keep the stream open, do not call onCompleted
    }

    /**
     * Get configurations by keys or prefix.
     */
    @Override
    public void getConfigs(GetConfigsRequest request, StreamObserver<GetConfigsResponse> responseObserver) {
        Map<String, String> configs;

        if (!request.getKeysList().isEmpty()) {
            // Get specific keys
            configs = new HashMap<>();
            for (String key : request.getKeysList()) {
                valueService.getValueByCode(key)
                    .ifPresent(value -> configs.put(key, value));
            }
        } else if (!request.getPrefix().isEmpty()) {
            // Get by prefix
            configs = valueService.getValuesByPrefix(request.getPrefix());
        } else {
            // Get all
            configs = valueService.getAllValues();
        }

        GetConfigsResponse response = GetConfigsResponse.newBuilder()
            .putAllConfigs(configs)
            .setVersion(System.currentTimeMillis())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Publish a configuration change.
     */
    @Override
    public void publishConfig(PublishConfigRequest request, StreamObserver<PublishConfigResponse> responseObserver) {
        String key = request.getKey();
        String value = request.getValue();
        String reason = request.getReason();
        String operator = request.getOperator();
        String serviceName = request.getServiceName();
        String environment = request.getEnvironment();
        String displayName = request.getDisplayName();
        String metaType = request.getType();
        String metaDefaultValue = request.getDefaultValue();
        boolean metaDeprecated = request.getDeprecated();

        log.info("Publish config request: key={}, value={}, default={}, service={}, env={}, type={}, deprecated={}",
                key, value, metaDefaultValue, serviceName, environment, metaType, metaDeprecated);

        try {
            // Find the config item by code, create if not exists
            ConfigItem item = itemService.findByCode(key).orElse(null);

            if (item == null) {
                // Auto-create config item with service group and metadata
                item = createConfigItem(key, serviceName, environment, displayName, metaType, metaDefaultValue, metaDeprecated);
                log.info("Auto-created config item: {} in group {} (type={}, default={})",
                        key, serviceName + "-" + environment, metaType, metaDefaultValue);
            } else {
                // Update metadata if provided
                boolean needsUpdate = false;
                if (!displayName.isEmpty() && !displayName.equals(item.getName())) {
                    item.setName(displayName);
                    needsUpdate = true;
                }
                if (!metaType.isEmpty() && !metaType.equals(item.getType())) {
                    item.setType(metaType);
                    needsUpdate = true;
                }
                if (!metaDefaultValue.isEmpty() && !metaDefaultValue.equals(item.getDefaultValue())) {
                    item.setDefaultValue(metaDefaultValue);
                    needsUpdate = true;
                }
                if (metaDeprecated && (item.getIsDeprecated() == null || !item.getIsDeprecated())) {
                    item.setIsDeprecated(true);
                    needsUpdate = true;
                }
                if (needsUpdate) {
                    item.setUpdatedAt(Instant.now());
                    itemService.update(item.getId(), item);
                    log.debug("Updated config item metadata: {}", key);
                }
            }

            // Skip deprecated items (don't update value)
            if (item.getIsDeprecated() != null && item.getIsDeprecated()) {
                log.debug("Skipping value update for deprecated config: {}", key);
                responseObserver.onNext(PublishConfigResponse.newBuilder()
                    .setSuccess(true)
                    .setVersion(System.currentTimeMillis())
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Check if value is empty and we have a value to sync
            String existingValue = valueService.findByItemId(item.getId())
                .map(v -> v.getValue())
                .orElse(null);

            log.debug("Processing config {}: value={}, existingValue={}, defaultValue={}",
                    key, value, existingValue, metaDefaultValue);

            // If value is empty and we have default value, use default
            if ((value == null || value.isEmpty()) && metaDefaultValue != null && !metaDefaultValue.isEmpty()) {
                value = metaDefaultValue;
                log.debug("Using default value for {}: {}", key, value);
            }

            // Update value if:
            // 1. Value is not empty
            // 2. Or we want to sync the current value (even if empty, to clear existing)
            if (value != null && !value.isEmpty()) {
                log.info("Updating config value for {}: {}", key, value);
                // Update the config value via service (includes history recording and event publishing)
                valueService.updateValue(item.getId(), value, reason, operator);

                // Push change notification to gRPC clients
                streamManager.pushConfigChange(key, value, ChangeType.CHANGE_TYPE_UPDATE);
            } else if (existingValue == null) {
                // No existing value and no new value, create empty record
                log.debug("Creating empty config value for: {}", key);
                valueService.updateValue(item.getId(), "", reason, operator);
            } else {
                log.debug("Skipping config value update for {}: value is empty and existing value exists", key);
            }

            responseObserver.onNext(PublishConfigResponse.newBuilder()
                .setSuccess(true)
                .setVersion(System.currentTimeMillis())
                .build());
        } catch (Exception e) {
            log.error("Failed to publish config: {}", key, e);
            responseObserver.onNext(PublishConfigResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build());
        }

        responseObserver.onCompleted();
    }

    // ==================== Service Registration & Discovery ====================

    /**
     * Register a service instance.
     */
    @Override
    public void registerService(RegisterServiceRequest request, StreamObserver<RegisterServiceResponse> responseObserver) {
        String serviceName = request.getServiceName();
        String host = request.getHost();
        int port = request.getPort();
        String instanceId = UUID.randomUUID().toString();
        String protocol = "grpc";
        int weight = 100;
        String metadata = "";

        log.info("RegisterService request: service={}, instance={}, host={}:{}",
                serviceName, instanceId, host, port);

        try {
            ServiceInstance instance = registryService.register(
                    serviceName, instanceId, host, port, protocol, weight, metadata);

            responseObserver.onNext(RegisterServiceResponse.newBuilder()
                    .setInstanceId(instance.getInstanceId())
                    .build());
        } catch (Exception e) {
            log.error("Failed to register service: {}", serviceName, e);
            responseObserver.onNext(RegisterServiceResponse.newBuilder()
                    .setInstanceId("")
                    .build());
        }

        responseObserver.onCompleted();
    }

    /**
     * Deregister a service instance.
     */
    @Override
    public void deregisterService(DeregisterServiceRequest request, StreamObserver<DeregisterServiceResponse> responseObserver) {
        String instanceId = request.getInstanceId();
        log.info("DeregisterService request: instance={}", instanceId);

        try {
            registryService.deregister(instanceId);
            responseObserver.onNext(DeregisterServiceResponse.newBuilder()
                    .setSuccess(true)
                    .build());
        } catch (Exception e) {
            log.error("Failed to deregister service instance: {}", instanceId, e);
            responseObserver.onNext(DeregisterServiceResponse.newBuilder()
                    .setSuccess(false)
                    .build());
        }

        responseObserver.onCompleted();
    }

    /**
     * Heartbeat stream for service health monitoring.
     */
    @Override
    public StreamObserver<HeartbeatRequest> heartbeatStream(StreamObserver<HeartbeatResponse> responseObserver) {
        return new StreamObserver<>() {
            private String instanceId;

            @Override
            public void onNext(HeartbeatRequest request) {
                instanceId = request.getInstanceId();
                heartbeatManager.processHeartbeat(instanceId);

                responseObserver.onNext(HeartbeatResponse.newBuilder()
                        .setAck(true)
                        .setServerTimestamp(System.currentTimeMillis())
                        .build());
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Heartbeat stream error for instance: {}", instanceId, t);
            }

            @Override
            public void onCompleted() {
                log.debug("Heartbeat stream completed for instance: {}", instanceId);
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * Discover services by name.
     */
    @Override
    public void discoverServices(DiscoverServicesRequest request, StreamObserver<DiscoverServicesResponse> responseObserver) {
        String serviceName = request.getServiceName();
        log.info("DiscoverServices request: service={}", serviceName);

        try {
            List<ServiceInstance> instances = registryService.discover(serviceName);

            DiscoverServicesResponse.Builder responseBuilder = DiscoverServicesResponse.newBuilder();
            for (ServiceInstance instance : instances) {
                ServiceInstanceMessage info = ServiceInstanceMessage.newBuilder()
                        .setInstanceId(instance.getInstanceId())
                        .setServiceName(serviceName)
                        .setHost(instance.getHost())
                        .setPort(instance.getPort())
                        .setStatus(instance.getStatus() != null && instance.getStatus() == 1 ? "UP" : "DOWN")
                        .build();
                responseBuilder.addInstances(info);
            }

            responseObserver.onNext(responseBuilder.build());
        } catch (Exception e) {
            log.error("Failed to discover services: {}", serviceName, e);
            responseObserver.onNext(DiscoverServicesResponse.newBuilder().build());
        }

        responseObserver.onCompleted();
    }

    // ==================== Private Helper Methods ====================

    /**
     * Create a new config item with the given code and metadata.
     */
    private ConfigItem createConfigItem(String code, String serviceName, String environment,
                                        String displayName, String metaType, String metaDefaultValue,
                                        boolean metaDeprecated) {
        Instant now = Instant.now();

        // Get or create group by service name and environment
        String groupCode = (serviceName.isEmpty() ? "default" : serviceName) + "-" +
                          (environment.isEmpty() ? "dev" : environment);
        ConfigGroup group = groupService.getOrCreate(groupCode, serviceName, environment);

        ConfigItem item = new ConfigItem();
        item.setCode(code);
        item.setName(displayName.isEmpty() ? code : displayName);
        item.setDescription("Auto-created from client registration");
        // 使用元数据中的类型，如果没有则默认为 STRING
        item.setType(metaType.isEmpty() ? "STRING" : mapTypeToSimple(metaType));
        item.setDefaultValue(metaDefaultValue.isEmpty() ? null : metaDefaultValue);
        item.setIsDeprecated(metaDeprecated);
        item.setGroupId(group.getId());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return itemService.create(item);
    }

    /**
     * 将 Java 类型映射为简单类型名称
     */
    private String mapTypeToSimple(String javaType) {
        if (javaType == null || javaType.isEmpty()) {
            return "STRING";
        }
        // Boolean
        if (javaType.equals("java.lang.Boolean") || javaType.equals("boolean")) {
            return "BOOLEAN";
        }
        // Integer
        if (javaType.equals("java.lang.Integer") || javaType.equals("int")) {
            return "INTEGER";
        }
        // Long
        if (javaType.equals("java.lang.Long") || javaType.equals("long")) {
            return "LONG";
        }
        // Double/Float
        if (javaType.equals("java.lang.Double") || javaType.equals("double") ||
            javaType.equals("java.lang.Float") || javaType.equals("float")) {
            return "DOUBLE";
        }
        // Duration
        if (javaType.equals("java.time.Duration")) {
            return "DURATION";
        }
        // List
        if (javaType.startsWith("java.util.List")) {
            return "LIST";
        }
        // Map
        if (javaType.startsWith("java.util.Map")) {
            return "MAP";
        }
        // 默认为 STRING
        return "STRING";
    }
}
