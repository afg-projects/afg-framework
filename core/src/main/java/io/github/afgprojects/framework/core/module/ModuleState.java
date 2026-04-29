package io.github.afgprojects.framework.core.module;

/**
 * 模块状态枚举
 * 定义模块生命周期的各个状态
 */
public enum ModuleState {

    /**
     * 已注册 - 模块已注册但尚未初始化
     */
    REGISTERED(0),

    /**
     * 初始化中 - 模块正在执行初始化逻辑
     */
    INITIALIZING(1),

    /**
     * 已就绪 - 模块初始化完成，正常运行
     */
    READY(2),

    /**
     * 暂停中 - 模块已暂停，可恢复
     */
    PAUSED(3),

    /**
     * 停止中 - 模块正在停止
     */
    STOPPING(4),

    /**
     * 已停止 - 模块已停止，不可用
     */
    STOPPED(5),

    /**
     * 失败 - 模块初始化或运行失败
     */
    FAILED(6);

    private final int order;

    ModuleState(int order) {
        this.order = order;
    }

    /**
     * 获取状态顺序
     */
    public int getOrder() {
        return order;
    }

    /**
     * 判断模块是否可操作（可提供服务）
     */
    public boolean isOperational() {
        return this == READY || this == PAUSED;
    }

    /**
     * 判断是否可以转换到目标状态
     *
     * @param target 目标状态
     * @return 如果可以转换返回 true
     */
    public boolean canTransitionTo(ModuleState target) {
        return switch (this) {
            case REGISTERED -> target == INITIALIZING || target == FAILED;
            case INITIALIZING -> target == READY || target == FAILED;
            case READY -> target == PAUSED || target == STOPPING || target == FAILED;
            case PAUSED -> target == READY || target == STOPPING;
            case STOPPING -> target == STOPPED || target == FAILED;
            case STOPPED -> target == INITIALIZING;
            case FAILED -> target == INITIALIZING;
        };
    }
}
