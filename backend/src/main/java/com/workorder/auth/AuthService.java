package com.workorder.auth;

import com.workorder.migration.LegacyMigrationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JdbcTemplate jdbc;
    private final LegacyMigrationService migrationService;
    private final Map<String, UserAccount> users;

    public AuthService(PasswordEncoder passwordEncoder, JwtService jwtService, JdbcTemplate jdbc, LegacyMigrationService migrationService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jdbc = jdbc;
        this.migrationService = migrationService;
        this.users = Map.of(
                "admin", new UserAccount(1L, "admin", "系统管理员", passwordEncoder.encode("admin123"),
                        Set.of("ADMIN"), Set.of("equipment:read", "equipment:write", "equipment:assign", "user:manage", "config:manage")),
                "agent", new UserAccount(2L, "agent", "工单坐席", passwordEncoder.encode("agent123"),
                        Set.of("AGENT"), Set.of("equipment:read", "equipment:write"))
        );
    }

    public AuthDtos.LoginResponse login(String username, String password) {
        UserAccount legacyUser = legacyLogin(username, password);
        if (legacyUser != null) {
            return new AuthDtos.LoginResponse(jwtService.issue(legacyUser), profile(legacyUser), menus(legacyUser.permissions()));
        }
        UserAccount user = users.get(username);
        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return new AuthDtos.LoginResponse(jwtService.issue(user), profile(user), menus(user.permissions()));
    }

    private AuthDtos.UserProfile profile(UserAccount user) {
        return new AuthDtos.UserProfile(user.id(), user.username(), user.displayName(), user.roles(), user.permissions());
    }

    private UserAccount legacyLogin(String username, String password) {
        migrationService.ensureMigrated();
        if (!tableExists("fa_admin")) {
            return null;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select id, username, nickname, password, salt, status
                from fa_admin
                where username=?
                """, username);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.getFirst();
        if (!"normal".equals(String.valueOf(row.get("status")))) {
            throw new IllegalArgumentException("账号已禁用");
        }
        String expected = md5(md5(password) + String.valueOf(row.get("salt")));
        if (!expected.equalsIgnoreCase(String.valueOf(row.get("password")))) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        Long adminId = number(row.get("id"));
        Set<String> roles = legacyRoles(adminId);
        Set<String> permissions = legacyPermissions(adminId);
        return new UserAccount(adminId, String.valueOf(row.get("username")),
                blank(String.valueOf(row.get("nickname"))) ? String.valueOf(row.get("username")) : String.valueOf(row.get("nickname")),
                String.valueOf(row.get("password")), roles, permissions);
    }

    private Set<String> legacyRoles(Long adminId) {
        Set<String> roles = new LinkedHashSet<>(jdbc.queryForList("""
                select g.name
                from fa_auth_group_access ga
                join fa_auth_group g on g.id=ga.group_id
                where ga.uid=? and g.status='normal'
                order by g.id asc
                """, String.class, adminId));
        if (roles.isEmpty()) {
            roles.add("LegacyAdmin");
        }
        return roles;
    }

    private Set<String> legacyPermissions(Long adminId) {
        List<String> rules = jdbc.queryForList("""
                select g.rules
                from fa_auth_group_access ga
                join fa_auth_group g on g.id=ga.group_id
                where ga.uid=? and g.status='normal'
                """, String.class, adminId);
        if (rules.stream().anyMatch("*"::equals)) {
            return new LinkedHashSet<>(jdbc.queryForList("select name from fa_auth_rule where status='normal'", String.class));
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String rule : rules) {
            if (rule == null || rule.isBlank()) {
                continue;
            }
            for (String id : rule.split(",")) {
                if (!id.isBlank()) {
                    ids.add(Long.parseLong(id.trim()));
                }
            }
        }
        if (ids.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        return new LinkedHashSet<>(jdbc.queryForList("select name from fa_auth_rule where status='normal' and id in (" + placeholders + ")", String.class, ids.toArray()));
    }

    private List<AuthDtos.MenuItem> menus(Set<String> permissions) {
        return List.of(
                new AuthDtos.MenuItem("工作台", "/dashboard", "Monitor", "dashboard/index"),
                new AuthDtos.MenuItem("设备档案", "/archives", "Folder", "equipment/archive"),
                new AuthDtos.MenuItem("设备列表", "/equipment", "List", "equipment/equipment"),
                new AuthDtos.MenuItem("维修工单", "/repairs", "Tools", "equipment/repair"),
                new AuthDtos.MenuItem("巡检计划", "/inspection", "Refresh", "equipment/inspection"),
                new AuthDtos.MenuItem("保养计划", "/maintenance", "Clock", "equipment/maintenance"),
                new AuthDtos.MenuItem("设备记录", "/records", "Tickets", "equipment/record"),
                new AuthDtos.MenuItem("用户角色", "/users", "User", "auth/admin"),
                new AuthDtos.MenuItem("员工管理", "/staff", "UserFilled", "equipment/staff"),
                new AuthDtos.MenuItem("部门管理", "/departments", "OfficeBuilding", "equipment/department"),
                new AuthDtos.MenuItem("供应商管理", "/suppliers", "Van", "equipment/supplier"),
                new AuthDtos.MenuItem("故障原因", "/failure-causes", "Warning", "equipment/failure_cause"),
                new AuthDtos.MenuItem("提醒人员", "/reminder-users", "Bell", "equipment/reminder_users"),
                new AuthDtos.MenuItem("系统配置", "/settings", "Setting", "general/config")
        ).stream().filter(item -> permissions.contains(item.permission())).toList();
    }

    private boolean tableExists(String tableName) {
        Integer found = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema=database() and table_name=?
                """, Integer.class, tableName);
        return found != null && found > 0;
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    private Long number(Object value) {
        return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank() || "null".equals(value);
    }
}
