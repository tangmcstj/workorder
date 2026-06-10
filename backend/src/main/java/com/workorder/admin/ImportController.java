package com.workorder.admin;

import com.workorder.common.ApiResponse;
import com.workorder.migration.LegacyMigrationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/import")
public class ImportController {
    private final JdbcTemplate jdbc;
    private final LegacyMigrationService migrationService;

    public ImportController(JdbcTemplate jdbc, LegacyMigrationService migrationService) {
        this.jdbc = jdbc;
        this.migrationService = migrationService;
    }

    @PostMapping("/archives")
    @Transactional
    public ApiResponse<Map<String, Object>> importArchives(@RequestParam("file") MultipartFile file) throws IOException {
        migrationService.ensureMigrated();
        if (file.isEmpty()) {
            throw new IllegalArgumentException("导入文件不能为空");
        }
        List<Map<String, String>> rows = rows(new String(file.getBytes(), StandardCharsets.UTF_8));
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String model = value(row, "model", "设备型号");
            String name = value(row, "name", "设备名称");
            if (blank(model) || blank(name)) {
                skipped++;
                errors.add("第 " + (i + 2) + " 行缺少 model/name");
                continue;
            }
            int amount = number(value(row, "amount", "设备数量"), 1);
            String supplier = value(row, "supplier", "供应商");
            String responsible = value(row, "responsible", "负责人");
            Long supplierId = blank(supplier) ? null : idByName("supplier", supplier);
            Long responsibleId = blank(responsible) ? null : userId(responsible);
            String idText = value(row, "id", "ID");
            if (!blank(idText)) {
                int count = jdbc.update("""
                        update equipment_archive
                        set model=?, name=?, amount=?, supplier_id=?, region=?, responsible_user_id=?, status=?, updated_at=now()
                        where id=? and deleted_at is null
                        """, model, name, amount, supplierId, value(row, "region", "所在区域"), responsibleId, status(row), Long.parseLong(idText));
                if (count > 0) {
                    updated++;
                    continue;
                }
            }
            jdbc.update("""
                    insert into equipment_archive
                    (legacy_id, model, name, parameter, amount, supplier_id, purchase_time, region, responsible_user_id, document, remark, status, created_at, updated_at)
                    values (null, ?, ?, ?, ?, ?, null, ?, ?, ?, ?, ?, now(), now())
                    """, model, name, value(row, "parameter", "参数"), amount, supplierId, value(row, "region", "所在区域"),
                    responsibleId, value(row, "document", "文档"), value(row, "remark", "备注"), status(row));
            inserted++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return ApiResponse.ok(result);
    }

    private List<Map<String, String>> rows(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.startsWith("\ufeff")) {
            normalized = normalized.substring(1);
        }
        List<List<String>> parsed = parseCsv(normalized);
        if (parsed.isEmpty()) {
            return List.of();
        }
        List<String> headers = parsed.getFirst();
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < parsed.size(); i++) {
            List<String> values = parsed.get(i);
            if (values.stream().allMatch(String::isBlank)) {
                continue;
            }
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j).trim(), j < values.size() ? values.get(j).trim() : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n') {
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        if (!cell.isEmpty() || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    private String value(Map<String, String> row, String... names) {
        for (String name : names) {
            String value = row.get(name);
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private Long idByName(String table, String name) {
        List<Long> ids = jdbc.queryForList("select id from " + table + " where name=? and deleted_at is null limit 1", Long.class, name);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private Long userId(String name) {
        List<Long> ids = jdbc.queryForList("select id from app_user where username=? or nickname=? limit 1", Long.class, name, name);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private int number(String value, int fallback) {
        try {
            return blank(value) ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String status(Map<String, String> row) {
        String status = value(row, "status", "状态");
        return blank(status) ? "normal" : status;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
