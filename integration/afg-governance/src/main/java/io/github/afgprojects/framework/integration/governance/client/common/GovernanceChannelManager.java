package io.github.afgprojects.framework.integration.governance.client.common;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;

/**
 * Governance 客户端通道管理器
 *
 * @author afg-projects
 */
public class GovernanceChannelManager {

    @Getter
    private final ManagedChannel channel;

    public GovernanceChannelManager(GovernanceCommonProperties properties) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(properties.getServerAddr())
                .usePlaintext();

        if (properties.isSignatureEnabled() && properties.getSignatureSecret() != null) {
            channelBuilder.intercept(new SignatureClientInterceptor(
                    properties.getSignatureKeyId(),
                    properties.getSignatureSecret()
            ));
        }

        this.channel = channelBuilder.build();
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}