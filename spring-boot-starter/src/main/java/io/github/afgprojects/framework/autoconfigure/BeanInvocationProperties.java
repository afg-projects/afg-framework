package io.github.afgprojects.framework.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "afg.invocation")
public class BeanInvocationProperties {
    private boolean enabled = true;
    private int asyncPoolSize = 4;
    private boolean interceptorsEnabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getAsyncPoolSize() { return asyncPoolSize; }
    public void setAsyncPoolSize(int asyncPoolSize) { this.asyncPoolSize = asyncPoolSize; }
    public boolean isInterceptorsEnabled() { return interceptorsEnabled; }
    public void setInterceptorsEnabled(boolean interceptorsEnabled) { this.interceptorsEnabled = interceptorsEnabled; }
}
