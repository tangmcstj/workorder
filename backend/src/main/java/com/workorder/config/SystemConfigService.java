package com.workorder.config;

import com.workorder.migration.LegacyMigrationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemConfigService {
    private final JdbcTemplate jdbc;
    private final LegacyMigrationService migrationService;

    public SystemConfigService(JdbcTemplate jdbc, LegacyMigrationService migrationService) {
        this.jdbc = jdbc;
        this.migrationService = migrationService;
    }

    public SystemConfigDtos.SystemConfig getConfig() {
        ensureTable();
        migrationService.ensureMigrated();
        Map<String, String> values = values();
        return new SystemConfigDtos.SystemConfig(
                values.getOrDefault("manage_phone", ""),
                values.getOrDefault("weappid", ""),
                mask(values.getOrDefault("weappsecret", "")),
                values.getOrDefault("qrcode_domain", ""),
                "1".equals(values.getOrDefault("repair_assign_oneself", "0")),
                count("select count(*) from reminder_user where deleted_at is null and status='normal'"),
                count("select count(*) from staff where deleted_at is null and status='normal' and openid<>''")
        );
    }

    @Transactional
    public SystemConfigDtos.SystemConfig update(SystemConfigDtos.UpdateSystemConfig request) {
        ensureTable();
        upsert("manage_phone", clean(request.managePhone()));
        upsert("weappid", clean(request.weappId()));
        if (request.weappSecret() != null && !request.weappSecret().isBlank() && !request.weappSecret().contains("*")) {
            upsert("weappsecret", request.weappSecret().trim());
        }
        upsert("qrcode_domain", clean(request.qrcodeDomain()));
        upsert("repair_assign_oneself", request.repairAssignOneself() ? "1" : "0");
        return getConfig();
    }

    public Map<String, String> values() {
        ensureTable();
        Map<String, String> values = new LinkedHashMap<>();
        jdbc.query("select config_key, config_value from workorder_system_config", rs -> {
            values.put(rs.getString("config_key"), rs.getString("config_value"));
        });
        return values;
    }

    private void ensureTable() {
        jdbc.execute("""
                create table if not exists workorder_system_config (
                    config_key varchar(80) primary key,
                    config_value text null,
                    updatetime bigint null
                ) engine=InnoDB default charset=utf8mb4
                """);
    }

    private void upsert(String key, String value) {
        jdbc.update("""
                insert into workorder_system_config (config_key, config_value, updatetime)
                values (?, ?, unix_timestamp())
                on duplicate key update config_value=values(config_value), updatetime=values(updatetime)
                """, key, value);
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 6) {
            return "******";
        }
        return value.substring(0, 3) + "******" + value.substring(value.length() - 3);
    }
}
