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
        data.put("roles", legacyRoles());
        return ApiResponse.ok(data);
    }

    private List<Map<String, Object>> legacyRoles() {
        if (!tableExists("fa_auth_group")) {
            return List.of();
        }
        return jdbc.query("select id, name, status, rules from fa_auth_group order by id asc", this::rowMap);
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
