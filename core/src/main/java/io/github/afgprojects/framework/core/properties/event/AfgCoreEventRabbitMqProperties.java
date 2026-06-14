package io.github.afgprojects.framework.core.properties.event;

import lombok.Data;

/**
 * RabbitMQ 事件配置。
 */
@Data
public class AfgCoreEventRabbitMqProperties {

    private String host = "localhost";
    private int port = 5672;
    private String username;
    private String password;
    private String virtualHost = "/";
    private String exchange = "afg.events";
    private String queuePrefix = "afg.queue.";
    private AckMode ackMode = AckMode.AUTO;
    private int prefetchCount = 10;
    private boolean autoDeclare = true;
}
