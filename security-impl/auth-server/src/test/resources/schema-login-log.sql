-- 登录日志表
-- 用于存储用户登录和登出的审计记录

CREATE TABLE IF NOT EXISTS auth_login_log (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    username VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64),
    ip VARCHAR(64) NOT NULL,
    device_id VARCHAR(128),
    device_name VARCHAR(256),
    browser VARCHAR(256),
    os VARCHAR(256),
    location VARCHAR(256),
    result VARCHAR(32) NOT NULL,
    fail_reason VARCHAR(512),
    login_time TIMESTAMP NOT NULL,
    logout_time TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_user_id_login ON auth_login_log (user_id);
CREATE INDEX IF NOT EXISTS idx_tenant_id_login ON auth_login_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_login_time ON auth_login_log (login_time);
CREATE INDEX IF NOT EXISTS idx_username_login ON auth_login_log (username);
