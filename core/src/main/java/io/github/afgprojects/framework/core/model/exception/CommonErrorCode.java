package io.github.afgprojects.framework.core.model.exception;

/**
 * 通用错误码
 * 错误码范围：10000-19999
 * 注：成功响应使用 code=0，由 Results.success() 返回
 */
public enum CommonErrorCode implements ErrorCode {

    // ==================== 通用错误 (10001-10099) ====================
    FAIL(10001, "操作失败", ErrorCategory.BUSINESS),
    PARAM_ERROR(10002, "参数错误", ErrorCategory.BUSINESS),
    PARAM_MISSING(10003, "参数缺失", ErrorCategory.BUSINESS),
    PARAM_FORMAT_ERROR(10004, "参数格式错误", ErrorCategory.BUSINESS),

    // ==================== 资源错误 (10100-10199) ====================
    NOT_FOUND(10100, "资源不存在", ErrorCategory.BUSINESS),
    RESOURCE_EXISTS(10101, "资源已存在", ErrorCategory.BUSINESS),
    RESOURCE_LOCKED(10102, "资源已锁定", ErrorCategory.BUSINESS),

    // ==================== 请求错误 (10200-10299) ====================
    METHOD_NOT_ALLOWED(10200, "请求方法不支持", ErrorCategory.BUSINESS),
    UNSUPPORTED_MEDIA_TYPE(10201, "不支持的媒体类型", ErrorCategory.BUSINESS),
    REQUEST_TIMEOUT(10202, "请求超时", ErrorCategory.NETWORK),
    PAYLOAD_TOO_LARGE(10203, "请求体过大", ErrorCategory.BUSINESS),

    // ==================== 限流错误 (10300-10399) ====================
    TOO_MANY_REQUESTS(10300, "请求过于频繁", ErrorCategory.BUSINESS),
    RATE_LIMIT_EXCEEDED(10301, "超过限流阈值", ErrorCategory.BUSINESS),
    CIRCUIT_BREAKER_OPEN(10302, "熔断器已开启", ErrorCategory.SYSTEM),

    // ==================== 认证授权错误 (10400-10499) ====================
    UNAUTHORIZED(10400, "未登录或登录已过期", ErrorCategory.SECURITY),
    TOKEN_EXPIRED(10401, "Token已过期", ErrorCategory.SECURITY),
    TOKEN_INVALID(10402, "Token无效", ErrorCategory.SECURITY),
    FORBIDDEN(10403, "无权限访问", ErrorCategory.SECURITY),
    PERMISSION_DENIED(10404, "权限不足", ErrorCategory.SECURITY),
    ACCOUNT_DISABLED(10405, "账号已禁用", ErrorCategory.SECURITY),
    ACCOUNT_LOCKED(10406, "账号已锁定", ErrorCategory.SECURITY),
    PASSWORD_EXPIRED(10407, "密码已过期", ErrorCategory.SECURITY),

    // ==================== 数据层错误 (11000-11999) ====================
    ENTITY_NOT_FOUND(11000, "实体不存在", ErrorCategory.BUSINESS),
    ENTITY_ALREADY_EXISTS(11001, "实体已存在", ErrorCategory.BUSINESS),
    FIELD_NOT_FOUND(11002, "字段不存在", ErrorCategory.BUSINESS),
    TABLE_NOT_FOUND(11003, "表不存在", ErrorCategory.SYSTEM),
    DDL_ERROR(11004, "DDL执行失败", ErrorCategory.SYSTEM),
    QUERY_ERROR(11005, "查询执行失败", ErrorCategory.SYSTEM),
    DATA_INTEGRITY_VIOLATION(11006, "数据完整性冲突", ErrorCategory.BUSINESS),
    OPTIMISTIC_LOCK_ERROR(11007, "乐观锁冲突", ErrorCategory.BUSINESS),

    // ==================== 存储错误 (12000-12999) ====================
    FILE_NOT_FOUND(12000, "文件不存在", ErrorCategory.BUSINESS),
    FILE_UPLOAD_ERROR(12001, "文件上传失败", ErrorCategory.SYSTEM),
    FILE_DOWNLOAD_ERROR(12002, "文件下载失败", ErrorCategory.SYSTEM),
    FILE_TYPE_NOT_ALLOWED(12003, "文件类型不允许", ErrorCategory.BUSINESS),
    FILE_SIZE_EXCEEDED(12004, "文件大小超限", ErrorCategory.BUSINESS),
    STORAGE_FULL(12005, "存储空间不足", ErrorCategory.SYSTEM),

    // ==================== 任务错误 (13000-13999) ====================
    JOB_NOT_FOUND(13000, "任务不存在", ErrorCategory.BUSINESS),
    JOB_EXECUTION_ERROR(13001, "任务执行失败", ErrorCategory.SYSTEM),
    JOB_ALREADY_RUNNING(13002, "任务已在运行中", ErrorCategory.BUSINESS),
    JOB_PAUSED(13003, "任务已暂停", ErrorCategory.BUSINESS),
    JOB_DISABLED(13004, "任务已禁用", ErrorCategory.BUSINESS),

    // ==================== HTTP客户端错误 (14000-14999) ====================
    CLIENT_REQUEST_FAILED(14000, "HTTP请求失败", ErrorCategory.NETWORK),
    CLIENT_TIMEOUT(14001, "HTTP请求超时", ErrorCategory.NETWORK),
    CLIENT_CONNECT_FAILED(14002, "HTTP连接失败", ErrorCategory.NETWORK),
    CLIENT_RETRY_EXHAUSTED(14003, "HTTP重试耗尽", ErrorCategory.NETWORK),
    CLIENT_CIRCUIT_OPEN(14004, "HTTP熔断器开启", ErrorCategory.SYSTEM),

    // ==================== 模块错误 (15000-15999) ====================
    MODULE_NOT_FOUND(15000, "模块不存在", ErrorCategory.SYSTEM),
    MODULE_DUPLICATE(15001, "模块已存在", ErrorCategory.SYSTEM),
    MODULE_CIRCULAR_DEPENDENCY(15002, "模块循环依赖", ErrorCategory.SYSTEM),
    MODULE_INIT_FAILED(15003, "模块初始化失败", ErrorCategory.SYSTEM),

    // ==================== 配置错误 (16000-16999) ====================
    CONFIG_NOT_FOUND(16000, "配置不存在", ErrorCategory.SYSTEM),
    CONFIG_BINDING_ERROR(16001, "配置绑定失败", ErrorCategory.SYSTEM),

    // ==================== 功能开关错误 (17000-17999) ====================
    FEATURE_DISABLED(17000, "功能已禁用", ErrorCategory.BUSINESS),
    FEATURE_FALLBACK_FAILED(17001, "功能回退失败", ErrorCategory.SYSTEM),

    // ==================== 系统错误 (19000-19999) ====================
    SYSTEM_ERROR(19000, "系统异常", ErrorCategory.SYSTEM),
    INTERNAL_ERROR(19001, "内部错误", ErrorCategory.SYSTEM),
    SERVICE_UNAVAILABLE(19002, "服务不可用", ErrorCategory.SYSTEM),
    DEPENDENCY_ERROR(19003, "依赖服务异常", ErrorCategory.SYSTEM),
    CONFIG_ERROR(19004, "配置错误", ErrorCategory.SYSTEM);

    private final int code;
    private final String message;
    private final ErrorCategory category;

    CommonErrorCode(int code, String message, ErrorCategory category) {
        this.code = code;
        this.message = message;
        this.category = category;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ErrorCategory getCategory() {
        return category;
    }
}
