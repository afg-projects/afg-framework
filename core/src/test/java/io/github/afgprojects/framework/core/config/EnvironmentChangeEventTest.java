package io.github.afgprojects.framework.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

class EnvironmentChangeEventTest extends BaseUnitTest {

    @Test
    void should_createEvent_when_usingConstructor() {
        EnvironmentChangeEvent event = new EnvironmentChangeEvent("dev", "prod");

        assertThat(event).isNotNull();
        assertThat(event.oldEnvironment()).isEqualTo("dev");
        assertThat(event.newEnvironment()).isEqualTo("prod");
    }

    @Test
    void should_haveCorrectOldAndNewEnvironment() {
        String oldEnv = "staging";
        String newEnv = "production";

        EnvironmentChangeEvent event = new EnvironmentChangeEvent(oldEnv, newEnv);

        assertThat(event.oldEnvironment()).isEqualTo(oldEnv);
        assertThat(event.newEnvironment()).isEqualTo(newEnv);
    }

    @Test
    void should_havePositiveTimestamp() {
        EnvironmentChangeEvent event = new EnvironmentChangeEvent("dev", "prod");

        assertThat(event.timestamp()).isPositive();
    }

    @Test
    void should_haveCorrectTimestamp_when_eventCreated() {
        long beforeCreation = System.currentTimeMillis();
        EnvironmentChangeEvent event = new EnvironmentChangeEvent("dev", "prod");
        long afterCreation = System.currentTimeMillis();

        assertThat(event.timestamp()).isBetween(beforeCreation, afterCreation);
    }
}
