package io.github.afgprojects.framework.core.web.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoggingPropertiesTest {

    @Test
    void should_haveDefaultValues_when_created() {
        LoggingProperties properties = new LoggingProperties();

        assertThat(properties.getMdc()).isNotNull();
        assertThat(properties.getStructured()).isNotNull();
    }

    @Test
    void should_haveDefaultMdcValues_when_created() {
        LoggingProperties properties = new LoggingProperties();
        LoggingProperties.Mdc mdc = properties.getMdc();

        assertThat(mdc.isEnabled()).isTrue();
        assertThat(mdc.getFields()).containsExactly("traceId", "tenantId", "userId", "requestPath");
    }

    @Test
    void should_haveDefaultStructuredValues_when_created() {
        LoggingProperties properties = new LoggingProperties();
        LoggingProperties.Structured structured = properties.getStructured();

        assertThat(structured.isEnabled()).isFalse();
        assertThat(structured.isPrettyPrint()).isFalse();
    }

    @Test
    void should_setMdcEnabled_when_setEnabledCalled() {
        LoggingProperties properties = new LoggingProperties();
        properties.getMdc().setEnabled(false);

        assertThat(properties.getMdc().isEnabled()).isFalse();
    }

    @Test
    void should_setMdcFields_when_setFieldsCalled() {
        LoggingProperties properties = new LoggingProperties();
        properties.getMdc().setFields(new String[] {"traceId", "requestId"});

        assertThat(properties.getMdc().getFields()).containsExactly("traceId", "requestId");
    }

    @Test
    void should_setStructuredEnabled_when_setEnabledCalled() {
        LoggingProperties properties = new LoggingProperties();
        properties.getStructured().setEnabled(true);

        assertThat(properties.getStructured().isEnabled()).isTrue();
    }

    @Test
    void should_setPrettyPrint_when_setPrettyPrintCalled() {
        LoggingProperties properties = new LoggingProperties();
        properties.getStructured().setPrettyPrint(true);

        assertThat(properties.getStructured().isPrettyPrint()).isTrue();
    }

    @Test
    void should_haveDefaultFileValues_when_created() {
        LoggingProperties properties = new LoggingProperties();
        LoggingProperties.File file = properties.getFile();

        assertThat(file.getPath()).isEqualTo("./logs");
        assertThat(file.getMaxSize()).isEqualTo("100MB");
        assertThat(file.getMaxHistory()).isEqualTo(30);
        assertThat(file.getTotalSizeCap()).isEqualTo("10GB");
    }

    @Test
    void should_haveDefaultAsyncValues_when_created() {
        LoggingProperties properties = new LoggingProperties();
        LoggingProperties.Async async = properties.getAsync();

        assertThat(async.getQueueSize()).isEqualTo(512);
        assertThat(async.getDiscardingThreshold()).isEqualTo(0);
        assertThat(async.isIncludeCallerData()).isTrue();
        assertThat(async.isNeverBlock()).isTrue();
    }

    @Test
    void should_haveDefaultMaskSensitiveValue_when_created() {
        LoggingProperties properties = new LoggingProperties();

        assertThat(properties.isMaskSensitive()).isTrue();
    }

    @Test
    void should_setFileProperties_when_modified() {
        LoggingProperties properties = new LoggingProperties();
        properties.getFile().setPath("/var/log/app");
        properties.getFile().setMaxSize("200MB");
        properties.getFile().setMaxHistory(60);
        properties.getFile().setTotalSizeCap("20GB");

        assertThat(properties.getFile().getPath()).isEqualTo("/var/log/app");
        assertThat(properties.getFile().getMaxSize()).isEqualTo("200MB");
        assertThat(properties.getFile().getMaxHistory()).isEqualTo(60);
        assertThat(properties.getFile().getTotalSizeCap()).isEqualTo("20GB");
    }

    @Test
    void should_setAsyncProperties_when_modified() {
        LoggingProperties properties = new LoggingProperties();
        properties.getAsync().setQueueSize(1024);
        properties.getAsync().setDiscardingThreshold(100);
        properties.getAsync().setIncludeCallerData(false);
        properties.getAsync().setNeverBlock(false);

        assertThat(properties.getAsync().getQueueSize()).isEqualTo(1024);
        assertThat(properties.getAsync().getDiscardingThreshold()).isEqualTo(100);
        assertThat(properties.getAsync().isIncludeCallerData()).isFalse();
        assertThat(properties.getAsync().isNeverBlock()).isFalse();
    }
}
