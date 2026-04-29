package io.github.afgprojects.framework.core.web.shutdown;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 优雅关闭配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.shutdown")
@SuppressWarnings({"PMD.UncommentedEmptyConstructor", "PMD.FieldDeclarationsShouldBeAtStartOfClass"})
public class ShutdownProperties {

    private boolean enabled = true;
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 是否启用同一阶段内相同 order 的回调并行执行。
     * 开启后，同一阶段内 order 相同的回调将并行执行，不同 order 的回调按顺序执行。
     */
    private boolean parallelExecutionEnabled;

    private final List<Phase> phases = new ArrayList<>();

    /**
     * 默认构造函数
     */
    public ShutdownProperties() {
        phases.add(new Phase("drain", Duration.ofSeconds(10)));
        phases.add(new Phase("cleanup", Duration.ofSeconds(15)));
        phases.add(new Phase("force", Duration.ofSeconds(5)));
    }

    /**
     * 关闭阶段配置
     */
    @Data
    public static class Phase {
        private String name;
        private Duration timeout;

        /**
         * 默认构造函数
         */
        public Phase() {}

        public Phase(String name, Duration timeout) {
            this.name = name;
            this.timeout = timeout;
        }
    }
}
