package io.github.afgprojects.framework.security.auth.audit.model;

public enum SecurityEventType {
    LOGIN_FAILURE_EXCESSIVE,
    LOGIN_FROM_NEW_DEVICE,
    LOGIN_FROM_NEW_LOCATION,
    ACCOUNT_LOCKED,
    PASSWORD_CHANGED,
    SUSPICIOUS_IP
}
