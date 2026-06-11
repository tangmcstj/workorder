package com.workorder.admin;

import com.workorder.common.ApiResponse;
import com.workorder.migration.LegacyMigrationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDataController {
    private final JdbcTemplate jdbc;
    private final LegacyMigrationService migrationService;

    public AdminDataController(JdbcTemplate jdbc, LegacyMigrationService migrationService) {
        this.jdbc = jdbc;
        this.migrationService = migrationService;
    }

    @GetMapping("/users-overview")
    public ApiResponse<Map<String, Object>> usersOverview() {
        migrationService.ensureMigrated();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", jdbc.query("""
                select u.id, u.username, u.nickname, u.mobile, u.status,
                       coalesce(s.workno, '') workno, coalesce(s.position, '') position,
                       coalesce(d.name, '') department, coalesce(s.openid, '') openid
                from app_user u
                left join staff s on s.user_id=u.id and s.deleted_at is null
                left join department d on d.id=s.department_id
                order by u.id asc
                """, this::rowMap));
        data.put("staff", jdbc.query("""
                select s.id, s.user_id, s.workno, s.position, s.openid, s.status,
                       coalesce(u.nickname, u.username, '') nickname,
                       coalesce(d.name, '') department
                from staff s
                left join app_user u on u.id=s.user_id
                left join department d on d.id=s.department_id
                where s.deleted_at is null
                order by s.id asc
                """, this::rowMap));
        data.put("departments", jdbc.query("""
                select id, name, equipment_manage, status
                from department
                where deleted_at is null
                order by id asc
                """, this::rowMap));
        data.put("admins", legacyAdmins());
        data.put("roles", legacyRoles());
        data.put("rules", legacyRules());
        data.put("menus", legacyMenus());
        return ApiResponse.ok(data);
    }

    @GetMapping("/access-overview")
    public ApiResponse<Map<String, Object>> accessOverview() {
        migrationService.ensureMigrated();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("admins", legacyAdmins());
        data.put("roles", legacyRoles());
        data.put("rules", legacyRules());
        data.put("menus", legacyMenus());
        data.put("equipmentRules", tableExists("fa_auth_rule") ? jdbc.query("""
                select id, pid, type, name, title, icon, url, ismenu, menutype, weigh, status
                from fa_auth_rule
                where name like 'equipment/%'
                order by pid asc, weigh desc, id asc
                """, this::rowMap) : List.of());
        return ApiResponse.ok(data);
    }

    private List<Map<String, Object>> legacyAdmins() {
        if (!tableExists("fa_admin")) {
            return List.of();
        }
        return jdbc.query("""
                select a.id, a.username, a.nickname, a.email, a.mobile, a.status,
                       coalesce(group_concat(g.name order by g.id separator '，'), '') group_names
                from fa_admin a
                left join fa_auth_group_access ga on ga.uid=a.id
                left join fa_auth_group g on g.id=ga.group_id
                group by a.id
                order by a.id asc
                """, this::rowMap);
    }

    private List<Map<String, Object>> legacyRoles() {
        if (!tableExists("fa_auth_group")) {
            return List.of();
        }
        return jdbc.query("""
                select g.id, g.pid, g.name, g.status, g.rules,
                       case when g.rules='*' then (select count(*) from fa_auth_rule) else 1 + length(g.rules) - length(replace(g.rules, ',', '')) end rule_count
                from fa_auth_group g
                order by g.id asc
                """, this::rowMap);
    }

    private List<Map<String, Object>> legacyRules() {
        if (!tableExists("fa_auth_rule")) {
            return List.of();
        }
        return jdbc.query("""
                select id, pid, type, name, title, icon, url, ismenu, menutype, weigh, status
                from fa_auth_rule
                order by pid asc, weigh desc, id asc
                """, this::rowMap);
    }

    private List<Map<String, Object>> legacyMenus() {
        if (!tableExists("fa_auth_rule")) {
            return List.of();
        }
        return jdbc.query("""
                select id, pid, type, name, title, icon, url, ismenu, menutype, weigh, status
                from fa_auth_rule
                where ismenu=1 or pid=0
                order by pid asc, weigh desc, id asc
                """, this::rowMap);
    }

    private boolean tableExists(String tableName) {
        Integer found = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema=database() and table_name=?
                """, Integer.class, tableName);
        return found != null && found > 0;
    }

    private Map<String, Object> rowMap(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }
}
