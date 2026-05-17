package io.github.afgprojects.framework.integration.governance.client;

import io.github.afgprojects.framework.integration.governance.api.*;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Governance gRPC 客户端
 * <p>
 * 提供配置获取、发布和订阅功能，支持签名认证。
 *
 * @author afg-projects
 */
@Slf4j
public class GovernanceClient {

    private static final Metadata.Key<String> KEY_SIGNATURE =
            Metadata.Key.of("x-signature", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_TIMESTAMP =
            Metadata.Key.of("x-timestamp", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_NONCE =
            Metadata.Key.of("x-nonce", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_KEY_ID =
            Metadata.Key.of("x-key-id", Metadata.ASCII_STRING_MARSHALLER);

    private final GovernanceClientProperties properties;
    private final ManagedChannel channel;
    private final GovernanceServiceGrpc.GovernanceServiceBlockingStub blockingStub;
    private final GovernanceServiceGrpc.GovernanceServiceStub asyncStub;

    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    // 签名认证相关
    private final SecureRandom secureRandom = new SecureRandom();

    public GovernanceClient(GovernanceClientProperties properties) {
        this.properties = properties;

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(properties.getServerAddr())
            .usePlaintext();

        // 如果启用了签名认证，添加客户端拦截器
        if (properties.isSignatureEnabled() && properties.getSignatureSecret() != null) {
            channelBuilder.intercept(new SignatureClientInterceptor(
                properties.getSignatureKeyId(),
                properties.getSignatureSecret()
            ));
            log.info("Signature authentication enabled for GovernanceClient");
        }

        this.channel = channelBuilder.build();
        this.blockingStub = GovernanceServiceGrpc.newBlockingStub(channel);
        this.asyncStub = GovernanceServiceGrpc.newStub(channel);
    }

    /**
     * 启动客户端
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        if (properties.isEnableConfigSubscribe()) {
            subscribeConfig();
        }

        log.info("GovernanceClient started, connected to {}", properties.getServerAddr());
    }

    /**
     * 停止客户端
     */
    public void stop() {
        running = false;
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("GovernanceClient stopped");
    }

    /**
     * 获取单个配置
     *
     * @param key 配置键
     * @return 配置值
     */
    public Optional<String> getConfig(String key) {
        // 先查缓存
        String cachedValue = configCache.get(key);
        if (cachedValue != null) {
            return Optional.of(cachedValue);
        }

        // 从服务端获取
        GetConfigsRequest request = GetConfigsRequest.newBuilder()
            .addKeys(key)
            .build();

        try {
            GetConfigsResponse response = blockingStub.getConfigs(request);
            String value = response.getConfigsMap().get(key);
            if (value != null) {
                configCache.put(key, value);
            }
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Failed to get config: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * 批量获取配置
     *
     * @param prefix 配置前缀
     * @return 配置键值对
     */
    public Map<String, String> getConfigs(String prefix) {
        GetConfigsRequest request = GetConfigsRequest.newBuilder()
            .setPrefix(prefix)
            .build();

        try {
            GetConfigsResponse response = blockingStub.getConfigs(request);
            configCache.putAll(response.getConfigsMap());
            return response.getConfigsMap();
        } catch (Exception e) {
            log.error("Failed to get configs with prefix: {}", prefix, e);
            return Map.of();
        }
    }

    /**
     * 批量获取指定键的配置
     *
     * @param keys 配置键列表
     * @return 配置键值对
     */
    public Map<String, String> getConfigs(java.util.List<String> keys) {
        GetConfigsRequest.Builder builder = GetConfigsRequest.newBuilder();
        keys.forEach(builder::addKeys);
        GetConfigsRequest request = builder.build();

        try {
            GetConfigsResponse response = blockingStub.getConfigs(request);
            configCache.putAll(response.getConfigsMap());
            return response.getConfigsMap();
        } catch (Exception e) {
            log.error("Failed to get configs for keys: {}", keys, e);
            return Map.of();
        }
    }

    /**
     * 发布配置
     *
     * @param key      配置键
     * @param value    配置值
     * @param reason   变更原因
     * @param operator 操作人
     * @return 是否成功
     */
    public boolean publishConfig(String key, String value, String reason, String operator) {
        PublishConfigRequest request = PublishConfigRequest.newBuilder()
            .setKey(key)
            .setValue(value)
            .setReason(reason != null ? reason : "")
            .setOperator(operator != null ? operator : "")
            .build();

        try {
            PublishConfigResponse response = blockingStub.publishConfig(request);
            if (response.getSuccess()) {
                configCache.put(key, value);
                return true;
            } else {
                log.error("Failed to publish config: {}, error: {}", key, response.getErrorMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to publish config: {}", key, e);
            return false;
        }
    }

    /**
     * 订阅配置变更
     */
    private void subscribeConfig() {
        SubscribeConfigRequest request = SubscribeConfigRequest.newBuilder()
            .setServiceName(properties.getServiceName() != null ? properties.getServiceName() : "")
            .build();

        asyncStub.subscribeConfig(request, new StreamObserver<>() {
            @Override
            public void onNext(ConfigChangeNotification notification) {
                log.info("Received config change: key={}, type={}",
                    notification.getKey(), notification.getChangeType());

                // 更新缓存
                if (notification.getChangeType() == ChangeType.CHANGE_TYPE_DELETE) {
                    configCache.remove(notification.getKey());
                } else {
                    configCache.put(notification.getKey(), notification.getValue());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Config subscription error", t);
                // 重连逻辑
                if (running) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        subscribeConfig();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            @Override
            public void onCompleted() {
                log.info("Config subscription completed");
            }
        });
    }

    /**
     * 获取配置缓存
     *
     * @return 配置缓存快照
     */
    public Map<String, String> getConfigCache() {
        return Map.copyOf(configCache);
    }

    /**
     * 刷新配置缓存
     */
    public void refreshCache() {
        try {
            GetConfigsResponse response = blockingStub.getConfigs(
                GetConfigsRequest.newBuilder().build()
            );
            configCache.clear();
            configCache.putAll(response.getConfigsMap());
            log.info("Config cache refreshed, size: {}", configCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh config cache", e);
        }
    }

    /**
     * 获取客户端状态
     *
     * @return 是否运行中
     */
    public boolean isRunning() {
        return running && !channel.isShutdown();
    }

    /**
     * 客户端签名拦截器
     */
    private class SignatureClientInterceptor implements ClientInterceptor {
        private final String keyId;
        private final String secret;

        SignatureClientInterceptor(String keyId, String secret) {
            this.keyId = keyId;
            this.secret = secret;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions,
                Channel next) {

            return new ForwardingClientCall.SimpleForwardingClientCall<>(
                    next.newCall(method, callOptions)) {

                @Override
                public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                    // 生成签名参数
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String nonce = generateNonce();
                    String methodDescriptor = method.getFullMethodName();

                    // 生成签名
                    String signature = generateSignature(timestamp, nonce, methodDescriptor);

                    // 添加签名信息到 Metadata
                    headers.put(KEY_SIGNATURE, signature);
                    headers.put(KEY_TIMESTAMP, timestamp);
                    headers.put(KEY_NONCE, nonce);
                    headers.put(KEY_KEY_ID, keyId);

                    log.debug("Added signature to request: method={}, keyId={}", methodDescriptor, keyId);

                    super.start(responseListener, headers);
                }
            };
        }

        private String generateNonce() {
            byte[] bytes = new byte[16];
            secureRandom.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

        private String generateSignature(String timestamp, String nonce, String body) {
            try {
                String signingString = timestamp + "\n" + nonce + "\n" + body;
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec keySpec = new SecretKeySpec(
                        secret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256");
                mac.init(keySpec);
                byte[] signature = mac.doFinal(signingString.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(signature);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate signature", e);
            }
        }
    }
}
