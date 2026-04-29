package io.github.afgprojects.framework.core.api.storage;

import java.io.InputStream;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.api.storage.model.DownloadResult;
import io.github.afgprojects.framework.core.api.storage.model.ListOptions;
import io.github.afgprojects.framework.core.api.storage.model.ListResult;
import io.github.afgprojects.framework.core.api.storage.model.PresignedUrlOptions;
import io.github.afgprojects.framework.core.api.storage.model.StorageMetadata;
import io.github.afgprojects.framework.core.api.storage.model.StorageObject;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.core.api.storage.model.UploadRequest;

/**
 * 文件存储接口
 * <p>
 * 提供统一的文件存储抽象，支持多种存储后端：
 * <ul>
 *   <li>本地文件系统</li>
 *   <li>MinIO</li>
 *   <li>阿里云 OSS</li>
 *   <li>AWS S3</li>
 * </ul>
 *
 * <p>所有实现应该是线程安全的。
 *
 * <p>具体实现由 integration/afg-storage 模块提供。
 */
public interface FileStorage {

    /**
     * 获取存储类型
     *
     * @return 存储类型
     */
    @NonNull
    StorageType getStorageType();

    /**
     * 获取存储桶名称（或根目录）
     *
     * @return 存储桶名称
     */
    @NonNull
    String getBucket();

    // ==================== 上传操作 ====================

    /**
     * 上传文件
     *
     * @param request 上传请求
     * @return 存储对象信息
     * @throws BusinessException 上传失败时抛出
     */
    @NonNull
    StorageObject upload(@NonNull UploadRequest request);

    /**
     * 上传文件（简化方法）
     *
     * @param key         文件唯一标识（路径）
     * @param inputStream 文件输入流
     * @param size        文件大小（字节）
     * @param contentType 内容类型
     * @return 存储对象信息
     */
    @NonNull
    default StorageObject upload(@NonNull String key, @NonNull InputStream inputStream,
                                 long size, @Nullable String contentType) {
        return upload(UploadRequest.of(key, inputStream, size, contentType));
    }

    // ==================== 下载操作 ====================

    /**
     * 下载文件
     *
     * @param key 文件唯一标识（路径）
     * @return 下载结果（包含输入流）
     * @throws BusinessException 文件不存在或下载失败时抛出
     */
    @NonNull
    DownloadResult download(@NonNull String key);

    // ==================== 删除操作 ====================

    /**
     * 删除文件
     *
     * @param key 文件唯一标识（路径）
     * @return 是否删除成功（文件不存在时返回 false）
     */
    boolean delete(@NonNull String key);

    /**
     * 批量删除文件
     *
     * @param keys 文件唯一标识列表
     * @return 成功删除的数量
     */
    int deleteBatch(@NonNull Iterable<String> keys);

    // ==================== 查询操作 ====================

    /**
     * 检查文件是否存在
     *
     * @param key 文件唯一标识（路径）
     * @return 是否存在
     */
    boolean exists(@NonNull String key);

    /**
     * 获取文件信息
     *
     * @param key 文件唯一标识（路径）
     * @return 存储对象信息，文件不存在时返回 null
     */
    @Nullable
    StorageObject get(@NonNull String key);

    /**
     * 列出文件
     *
     * @param options 列表选项
     * @return 列表结果
     */
    @NonNull
    ListResult list(@NonNull ListOptions options);

    /**
     * 列出指定前缀下的所有文件
     *
     * @param prefix 前缀
     * @return 列表结果
     */
    @NonNull
    default ListResult listByPrefix(@Nullable String prefix) {
        return list(ListOptions.withPrefix(prefix));
    }

    // ==================== URL 操作 ====================

    /**
     * 获取访问 URL
     *
     * @param key 文件唯一标识（路径）
     * @return 访问 URL
     * @throws BusinessException 文件不存在时抛出
     */
    @NonNull
    String getUrl(@NonNull String key);

    /**
     * 获取预签名 URL（用于临时访问）
     *
     * @param key     文件唯一标识（路径）
     * @param options 预签名选项
     * @return 预签名 URL
     */
    @NonNull
    String getPresignedUrl(@NonNull String key, @NonNull PresignedUrlOptions options);

    /**
     * 获取预签名下载 URL（默认 1 小时过期）
     *
     * @param key 文件唯一标识（路径）
     * @return 预签名 URL
     */
    @NonNull
    default String getPresignedUrl(@NonNull String key) {
        return getPresignedUrl(key, PresignedUrlOptions.forGet());
    }

    // ==================== 元数据操作 ====================

    /**
     * 更新文件元数据
     *
     * @param key      文件唯一标识（路径）
     * @param metadata 新的元数据
     * @return 更新后的存储对象信息
     */
    @NonNull
    StorageObject updateMetadata(@NonNull String key, @NonNull StorageMetadata metadata);

    /**
     * 复制文件
     *
     * @param sourceKey 源文件标识
     * @param targetKey 目标文件标识
     * @return 目标存储对象信息
     */
    @NonNull
    StorageObject copy(@NonNull String sourceKey, @NonNull String targetKey);

    /**
     * 移动文件
     *
     * @param sourceKey 源文件标识
     * @param targetKey 目标文件标识
     * @return 目标存储对象信息
     */
    @NonNull
    default StorageObject move(@NonNull String sourceKey, @NonNull String targetKey) {
        StorageObject result = copy(sourceKey, targetKey);
        delete(sourceKey);
        return result;
    }
}
