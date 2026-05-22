package io.github.afgprojects.framework.core.web.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * LoggingConfig 单元测试。
 * <p>
 * 测试日志配置属性的功能，验证 MDC 配置、结构化日志配置、文件配置和异步配置的默认值和设置。
 *
 * @see AfgCoreProperties.LoggingConfig
 */
class LoggingConfigTest {

    /**
     * 测试创建时具有默认值。
     */
    @Test
    void should_haveDefaultValues_when_created() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();

        assertThat(config.getMdc()).isNotNull();
        assertThat(config.getStructured()).isNotNull();
    }

    @Test
    void should_haveDefaultMdcValues_when_created() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        AfgCoreProperties.LoggingConfig.MdcConfig mdc = config.getMdc();

        assertThat(mdc.isEnabled()).isTrue();
        assertThat(mdc.getFields()).containsExactly("traceId", "tenantId", "userId", "requestPath");
    }

    @Test
    void should_haveDefaultStructuredValues_when_created() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        AfgCoreProperties.LoggingConfig.StructuredConfig structured = config.getStructured();

        assertThat(structured.isEnabled()).isFalse();
        assertThat(structured.isPrettyPrint()).isFalse();
    }

    @Test
    void should_setMdcEnabled_when_setEnabledCalled() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        config.getMdc().setEnabled(false);

        assertThat(config.getMdc().isEnabled()).isFalse();
    }

    @Test
    void should_setMdcFields_when_setFieldsCalled() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        config.getMdc().setFields(new String[] {"traceId", "requestId"});

        assertThat(config.getMdc().getFields()).containsExactly("traceId", "requestId");
    }

    @Test
    void should_setStructuredEnabled_when_setEnabledCalled() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        config.getStructured().setEnabled(true);

        assertThat(config.getStructured().isEnabled()).isTrue();
    }

    @Test
    void should_setPrettyPrint_when_setPrettyPrintCalled() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        config.getStructured().setPrettyPrint(true);

        assertThat(config.getStructured().isPrettyPrint()).isTrue();
    }

    @Test
    void should_haveDefaultFileValues_when_created() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        AfgCoreProperties.LoggingConfig.LogFileConfig file = config.getFile();

        assertThat(file.getPath()).isEqualTo("./logs");
        assertThat(file.getMaxSize()).isEqualTo("100MB");
        assertThat(file.getMaxHistory()).isEqualTo(30);
        assertThat(file.getTotalSizeCap()).isEqualTo("10GB");
    }

    @Test
    void should_haveDefaultAsyncValues_when_created() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        AfgCoreProperties.LoggingConfig.AsyncLogConfig async = config.getAsync();

        assertThat(async.getQueueSize()).isEqualTo(512);
        assertThat(async.getDiscardingThreshold()).isEqualTo(0);
        assertThat(async.isIncludeCallerData()).isTrue();
        assertThat(async.isNeverBlock()).isTrue();
    }

    @Test
    void should_haveDefaultMaskSensitiveValue_when_created() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();

        assertThat(config.isMaskSensitive()).isTrue();
    }

    @Test
    void should_setFileProperties_when_modified() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        config.getFile().setPath("/var/log/app");
        config.getFile().setMaxSize("200MB");
        config.getFile().setMaxHistory(60);
        config.getFile().setTotalSizeCap("20GB");

        assertThat(config.getFile().getPath()).isEqualTo("/var/log/app");
        assertThat(config.getFile().getMaxSize()).isEqualTo("200MB");
        assertThat(config.getFile().getMaxHistory()).isEqualTo(60);
        assertThat(config.getFile().getTotalSizeCap()).isEqualTo("20GB");
    }

    @Test
    void should_setAsyncProperties_when_modified() {
        AfgCoreProperties.LoggingConfig config = new AfgCoreProperties.LoggingConfig();
        config.getAsync().setQueueSize(1024);
        config.getAsync().setDiscardingThreshold(100);
        config.getAsync().setIncludeCallerData(false);
        config.getAsync().setNeverBlock(false);

        assertThat(config.getAsync().getQueueSize()).isEqualTo(1024);
        assertThat(config.getAsync().getDiscardingThreshold()).isEqualTo(100);
        assertThat(config.getAsync().isIncludeCallerData()).isFalse();
        assertThat(config.getAsync().isNeverBlock()).isFalse();
    }
}