package io.github.afgprojects.framework.core.properties.virtualthread;

import lombok.Data;

/**
 * 虚拟线程配置。
 */
@Data
public class AfgCoreVirtualThreadProperties {

    /**
     * 是否启用虚拟线程。
     */
    private boolean enabled = true;

    /**
     * 虚拟线程名前缀。
     */
    private String namePrefix = "afg-vt-";
}
