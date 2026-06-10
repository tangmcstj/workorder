package com.workorder.admin;

import com.workorder.migration.LegacyMigrationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/export")
public class ExportController {
    private final JdbcTemplate jdbc;
    private final LegacyMigrationService migrationService;

    public ExportController(JdbcTemplate jdbc, LegacyMigrationService migrationService) {
        this.jdbc = jdbc;
        this.migrationService = migrationService;
    }

    @GetMapping("/{type}.csv")
    public ResponseEntity<byte[]> export(@PathVariable String type) {
        migrationService.ensureMigrated();
        ExportSpec spec = spec(type);
        List<Map<String, Object>> rows = jdbc.query(spec.sql(), this::row);
        byte[] body = csv(spec.headers(), rows).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + spec.filename())
                .body(body);
    }

    private ExportSpec spec(String type) {
        return switch (type) {
            case "archives" -> new ExportSpec("archives.csv", List.of("id", "model", "name", "amount", "supplier", "region", "responsible", "status"), """
                    select a.id, a.model, a.name, a.amount, coalesce(s.name, '') supplier, a.region,
                           coalesce(u.nickname, u.username, '') responsible, a.status
                    from equipment_archive a
                    left join supplier s on s.id=a.supplier_id
                    left join app_user u on u.id=a.responsible_user_id
                    where a.deleted_at is null
                    order by a.id
                    """);
            case "equipment" -> new ExportSpec("equipment.csv", List.of("id", "coding", "equipment_code", "archive_model", "archive_name", "region", "work_status", "status"), """
                    select e.id, e.coding, e.equipment_code, a.model archive_model, a.name archive_name,
                           a.region, e.work_status, e.status
                    from equipment_item e
                    join equipment_archive a on a.id=e.archive_id
                    where e.deleted_at is null
                    order by e.id
                    """);
            case "archive-tags", "exportTag" -> new ExportSpec("archive-tags.csv", List.of("archive_id", "equipment_id", "coding", "equipment_code", "model", "name", "region", "qrcode_path"), """
                    select a.id archive_id, e.id equipment_id, e.coding, e.equipment_code, a.model, a.name,
                           a.region, concat('/pages/equipment/info?coding=', e.coding) qrcode_path
                    from equipment_item e
                    join equipment_archive a on a.id=e.archive_id
                    where e.deleted_at is null and a.deleted_at is null
                    order by a.id, e.id
                    """);
            case "repairs" -> new ExportSpec("repairs.csv", List.of("id", "repair_code", "equipment_code", "archive_name", "content", "register_user", "repair_user", "status"), """
                    select r.id, r.repair_code, e.equipment_code, a.name archive_name, r.content,
                           coalesce(ru.nickname, ru.username, '') register_user,
                           coalesce(mu.nickname, mu.username, '') repair_user,
                           r.status
                    from repair_order r
                    join equipment_item e on e.id=r.equipment_id
                    join equipment_archive a on a.id=r.archive_id
                    left join app_user ru on ru.id=r.register_user_id
                    left join app_user mu on mu.id=r.repair_user_id
                    where r.deleted_at is null
                    order by r.id
                    """);
            case "tasks" -> new ExportSpec("tasks.csv", List.of("id", "coding", "type", "equipment_code", "archive_name", "task_user", "status"), """
                    select t.id, t.coding, p.type, e.equipment_code, a.name archive_name,
                           coalesce(u.nickname, u.username, '') task_user, t.status
                    from plan_task t
                    join work_plan p on p.id=t.plan_id
                    join equipment_item e on e.id=t.equipment_id
                    join equipment_archive a on a.id=e.archive_id
                    left join app_user u on u.id=t.task_user_id
                    where t.deleted_at is null
                    order by t.id
                    """);
            default -> throw new IllegalArgumentException("不支持的导出类型");
        };
    }

    private Map<String, Object> row(ResultSet rs, int rowNum) throws SQLException {
        java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    private String csv(List<String> headers, List<Map<String, Object>> rows) {
        StringBuilder builder = new StringBuilder("\ufeff");
        builder.append(String.join(",", headers)).append('\n');
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(escape(row.get(headers.get(i))));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private record ExportSpec(String filename, List<String> headers, String sql) {
    }
}
