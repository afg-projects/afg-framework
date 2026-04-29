package io.github.afgprojects.framework.integration.redis.feature;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.feature.FeatureFlag;
import io.github.afgprojects.framework.core.feature.FeatureFlagManager;

/**
 * Redisson 分布式存储客户端
 * <p>
 * 使用 Redisson 的 RMap 存储功能开关状态，支持分布式环境
 * </p>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RedissonStorageClient implements FeatureFlagManager.DistributedStorageClient {

    private static final Logger log = LoggerFactory.getLogger(RedissonStorageClient.class);

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final String mapKey;

    /**
     * 构造函数
     *
     * @param redissonClient Redisson 客户端
     * @param objectMapper   JSON 序列化器
     * @param keyPrefix      键前缀
     */
    public RedissonStorageClient(
            @NonNull RedissonClient redissonClient,
            @NonNull ObjectMapper objectMapper,
            @NonNull String keyPrefix) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.mapKey = keyPrefix + "flags";
    }

    @Override
    public @Nullable FeatureFlag get(@NonNull String featureName) {
        try {
            RMap<String, String> map = redissonClient.getMap(mapKey);
            String json = map.get(featureName);
            if (json == null) {
                return null;
            }
            return deserialize(json);
        } catch (Exception e) {
            log.error("获取功能开关失败: {}", featureName, e);
            return null;
        }
    }

    @Override
    public void put(@NonNull String featureName, @NonNull FeatureFlag flag) {
        try {
            RMap<String, String> map = redissonClient.getMap(mapKey);
            map.put(featureName, serialize(flag));
            log.debug("功能开关已存储: {}", featureName);
        } catch (Exception e) {
            log.error("存储功能开关失败: {}", featureName, e);
        }
    }

    @Override
    public void putAll(@NonNull Map<String, FeatureFlag> flags) {
        try {
            RMap<String, String> map = redissonClient.getMap(mapKey);
            Map<String, String> jsonMap = new java.util.HashMap<>();
            for (Map.Entry<String, FeatureFlag> entry : flags.entrySet()) {
                jsonMap.put(entry.getKey(), serialize(entry.getValue()));
            }
            map.putAll(jsonMap);
            log.debug("批量存储功能开关: {} 个", flags.size());
        } catch (Exception e) {
            log.error("批量存储功能开关失败", e);
        }
    }

    @Override
    public void remove(@NonNull String featureName) {
        try {
            RMap<String, String> map = redissonClient.getMap(mapKey);
            map.remove(featureName);
            log.debug("功能开关已删除: {}", featureName);
        } catch (Exception e) {
            log.error("删除功能开关失败: {}", featureName, e);
        }
    }

    @Override
    @NonNull
    public Map<String, FeatureFlag> getAll() {
        Map<String, FeatureFlag> result = new java.util.HashMap<>();
        try {
            RMap<String, String> map = redissonClient.getMap(mapKey);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                FeatureFlag flag = deserialize(entry.getValue());
                if (flag != null) {
                    result.put(entry.getKey(), flag);
                }
            }
        } catch (Exception e) {
            log.error("获取所有功能开关失败", e);
        }
        return result;
    }

    /**
     * 序列化功能开关
     */
    private String serialize(FeatureFlag flag) throws Exception {
        return objectMapper.writeValueAsString(flag);
    }

    /**
     * 反序列化功能开关
     */
    private @Nullable FeatureFlag deserialize(String json) {
        try {
            return objectMapper.readValue(json, FeatureFlag.class);
        } catch (Exception e) {
            log.error("反序列化功能开关失败: {}", json, e);
            return null;
        }
    }

    /**
     * 功能开关变更监听器
     */
    @FunctionalInterface
    public interface FeatureFlagChangeListener {
        /**
         * 功能开关变更回调
         *
         * @param featureName 功能名称
         * @param flag        新的功能开关，null 表示删除
         */
        void onChanged(@NonNull String featureName, @Nullable FeatureFlag flag);
    }
}
