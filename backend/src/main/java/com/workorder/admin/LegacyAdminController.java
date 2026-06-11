package com.workorder.admin;

import com.workorder.common.ApiResponse;
import com.workorder.common.PageResult;
import com.workorder.migration.LegacyMigrationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin/legacy")
public class LegacyAdminController {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Map<String, ResourceSpec> RESOURCES = Map.of(
            "departments", new ResourceSpec("department", List.of("name", "equipment_manage", "status")),
            "suppliers", new ResourceSpec("supplier", List.of("supplier_code", "name", "relationship", "bank", "bank_account", "contact", "contact_mobile", "remark", "status")),
            "failure-causes", new ResourceSpec("failure_cause", List.of("name", "status")),
            "staff", new ResourceSpec("staff", List.of("user_id", "department_id", "workno", "position", "openid", "status")),
            "reminder-users", new ResourceSpec("reminder_user", List.of("staff_id", "type", "status")),
            "archives", new ResourceSpec("equipment_archive", List.of("model", "name", "parameter", "amount", "supplier_id", "purchase_time", "region", "responsible_user_id", "document", "remark", "status"))
    );

    private final JdbcTemplate jdbc;
    private final LegacyMigrationService migrationService;

    public LegacyAdminController(JdbcTemplate jdbc, LegacyMigrationService migrationService) {
        this.jdbc = jdbc;
        this.migrationService = migrationService;
    }

    @GetMapping("/{resource}")
    public ApiResponse<PageResult<Map<String, Object>>> list(@PathVariable String resource,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int size,
                                                             @RequestParam(defaultValue = "false") boolean recycle,
                                                             @RequestParam(required = false) String keyword) {
        ready();
        ResourceSpec spec = spec(resource);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        QueryParts query = listQuery(resource, spec, recycle, keyword);
        long total = count("select count(*) from " + spec.table + " x where " + query.where, query.args.toArray());
        List<Object> args = new ArrayList<>(query.args);
        args.add(safeSize);
        args.add((safePage - 1) * safeSize);
        List<Map<String, Object>> records = jdbc.query(query.sql + " limit ? offset ?", this::rowMap, args.toArray());
        return ApiResponse.ok(new PageResult<>(records, total, safePage, safeSize));
    }

    @PostMapping("/{resource}")
    @Transactional
    public ApiResponse<Map<String, Object>> add(@PathVariable String resource, @RequestBody Map<String, Object> body) {
        ready();
        ResourceSpec spec = spec(resource);
        Map<String, Object> values = permittedValues(spec, body);
        defaults(resource, values);
        long id = nextId(spec.table);
        List<String> columns = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        columns.add("id");
        args.add(id);
        values.forEach((key, value) -> {
            columns.add(key);
            args.add(value);
        });
        columns.add("created_at");
        columns.add("updated_at");
        String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());
        args.add(Instant.now());
        args.add(Instant.now());
        jdbc.update("insert into " + spec.table + " (" + String.join(", ", columns) + ") values (" + placeholders + ")", args.toArray());
        return ApiResponse.ok(getById(spec.table, id));
    }

    @PutMapping("/{resource}/{id}")
    @Transactional
    public ApiResponse<Map<String, Object>> edit(@PathVariable String resource, @PathVariable long id, @RequestBody Map<String, Object> body) {
        ready();
        ResourceSpec spec = spec(resource);
        Map<String, Object> values = permittedValues(spec, body);
        if (values.isEmpty()) {
            return ApiResponse.ok(getById(spec.table, id));
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("update " + spec.table + " set ");
        int index = 0;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (index++ > 0) {
                sql.append(", ");
            }
            sql.append(entry.getKey()).append("=?");
            args.add(entry.getValue());
        }
        sql.append(", updated_at=now() where id=?");
        args.add(id);
        jdbc.update(sql.toString(), args.toArray());
        return ApiResponse.ok(getById(spec.table, id));
    }

    @DeleteMapping("/{resource}/{id}")
    @Transactional
    public ApiResponse<Map<String, Object>> delete(@PathVariable String resource, @PathVariable long id) {
        ready();
        ResourceSpec spec = spec(resource);
        jdbc.update("update " + spec.table + " set deleted_at=now(), updated_at=now() where id=?", id);
        return ApiResponse.ok(Map.of("id", id, "deleted", true));
    }

    @PostMapping("/{resource}/{id}/restore")
    @Transactional
    public ApiResponse<Map<String, Object>> restore(@PathVariable String resource, @PathVariable long id) {
        ready();
        ResourceSpec spec = spec(resource);
        jdbc.update("update " + spec.table + " set deleted_at=null, updated_at=now() where id=?", id);
        return ApiResponse.ok(getById(spec.table, id));
    }

    @DeleteMapping("/{resource}/{id}/destroy")
    @Transactional
    public ApiResponse<Map<String, Object>> destroy(@PathVariable String resource, @PathVariable long id) {
        ready();
        ResourceSpec spec = spec(resource);
        int rows = jdbc.update("delete from " + spec.table + " where id=? and deleted_at is not null", id);
        return ApiResponse.ok(Map.of("id", id, "destroyed", rows > 0));
    }

    @PutMapping("/{resource}/multi")
    @Transactional
    public ApiResponse<Map<String, Object>> multi(@PathVariable String resource, @RequestBody Map<String, Object> body) {
        ready();
        ResourceSpec spec = spec(resource);
        List<?> ids = body.get("ids") instanceof List<?> list ? list : List.of();
        Map<String, Object> values = permittedValues(spec, body);
        if (ids.isEmpty() || values.isEmpty()) {
            return ApiResponse.ok(Map.of("updated", 0));
        }
        int updated = 0;
        for (Object id : ids) {
            Map<String, Object> copy = new LinkedHashMap<>(values);
            updated += updateValues(spec.table, number(id), copy);
        }
        return ApiResponse.ok(Map.of("updated", updated));
    }

    @PostMapping("/staff/{id}/unbind")
    @Transactional
    public ApiResponse<Map<String, Object>> unbindStaff(@PathVariable long id) {
        ready();
        jdbc.update("update staff set openid='', updated_at=now() where id=?", id);
        return ApiResponse.ok(Map.of("id", id, "openid", ""));
    }

    @GetMapping({"/staff-picker", "/staff/picker", "/staff/getPlanSelectpage"})
    public ApiResponse<List<Map<String, Object>>> staffPicker(@RequestParam(required = false) String keyword) {
        ready();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("s.deleted_at is null and s.status='normal'");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (u.nickname like ? or u.username like ? or s.workno like ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        return ApiResponse.ok(jdbc.query("""
                select s.id staff_id, s.user_id, s.workno, s.position, s.openid,
                       coalesce(u.nickname, u.username, '') nickname, coalesce(u.mobile, '') mobile,
                       coalesce(d.name, '') department
                from staff s
                left join app_user u on u.id=s.user_id
                left join department d on d.id=s.department_id
                where %s
                order by s.id asc
                """.formatted(where), this::rowMap, args.toArray()));
    }

    @GetMapping("/plans")
    public ApiResponse<PageResult<Map<String, Object>>> plans(@RequestParam(required = false) String type,
                                                              @RequestParam(defaultValue = "false") boolean deactivated,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        ready();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(deactivated ? "p.status<>'normal' or p.deleted_at is not null" : "p.status='normal' and p.deleted_at is null");
        if (type != null && !type.isBlank()) {
            where.insert(0, "(").append(") and p.type=?");
            args.add(type);
        }
        long total = count("select count(*) from work_plan p where " + where, args.toArray());
        args.add(safeSize);
        args.add((safePage - 1) * safeSize);
        List<Map<String, Object>> records = jdbc.query("""
                select p.*, count(distinct t.equipment_id) equipment_count,
                       sum(case when t.status='pending' and t.deleted_at is null then 1 else 0 end) pending_count,
                       sum(case when t.status='finish' and t.deleted_at is null then 1 else 0 end) finish_count
                from work_plan p
                left join plan_task t on t.plan_id=p.id
                where %s
                group by p.id
                order by p.id desc
                limit ? offset ?
                """.formatted(where), this::rowMap, args.toArray());
        return ApiResponse.ok(new PageResult<>(records, total, safePage, safeSize));
    }

    @PostMapping("/plans/{id}/stop")
    @Transactional
    public ApiResponse<Map<String, Object>> stopPlan(@PathVariable long id) {
        ready();
        jdbc.update("update work_plan set status='hidden', updated_at=now() where id=?", id);
        return ApiResponse.ok(getById("work_plan", id));
    }

    @GetMapping("/plans/{id}/fields")
    public ApiResponse<List<Map<String, Object>>> planFields(@PathVariable long id) {
        ready();
        return ApiResponse.ok(jdbc.query("select * from plan_field where plan_id=? and deleted_at is null order by sort asc, id asc", this::rowMap, id));
    }

    @GetMapping("/plans/{id}/equipment")
    public ApiResponse<List<Map<String, Object>>> planEquipment(@PathVariable long id) {
        ready();
        return ApiResponse.ok(jdbc.query("""
                select distinct e.id, e.coding, e.equipment_code, e.work_status, a.name archive_name, a.model archive_model
                from plan_task t
                join equipment_item e on e.id=t.equipment_id
                join equipment_archive a on a.id=e.archive_id
                where t.plan_id=? and t.deleted_at is null
                order by e.id asc
                """, this::rowMap, id));
    }

    @PostMapping("/plans/clear-invalid-task")
    @Transactional
    public ApiResponse<Map<String, Object>> clearInvalidTask(@RequestParam(defaultValue = "true") boolean dryRun) {
        ready();
        long invalid = count("""
                select count(*)
                from plan_task t
                left join work_plan p on p.id=t.plan_id
                left join equipment_item e on e.id=t.equipment_id
                where t.deleted_at is null and (p.id is null or p.deleted_at is not null or p.status<>'normal' or e.id is null or e.deleted_at is not null or e.work_status='scrapped')
                """);
        if (dryRun) {
            return ApiResponse.ok(Map.of("invalid", invalid, "cleared", 0, "dryRun", true));
        }
        int cleared = jdbc.update("""
                update plan_task t
                left join work_plan p on p.id=t.plan_id
                left join equipment_item e on e.id=t.equipment_id
                set t.deleted_at=now(), t.updated_at=now()
                where t.deleted_at is null and (p.id is null or p.deleted_at is not null or p.status<>'normal' or e.id is null or e.deleted_at is not null or e.work_status='scrapped')
                """);
        return ApiResponse.ok(Map.of("invalid", invalid, "cleared", cleared, "dryRun", false));
    }

    @GetMapping("/records")
    public ApiResponse<PageResult<Map<String, Object>>> records(@RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "20") int size,
                                                                @RequestParam(required = false) String type) {
        ready();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("r.deleted_at is null");
        if (type != null && !type.isBlank()) {
            where.append(" and r.type=?");
            args.add(type);
        }
        long total = count("select count(*) from equipment_record r where " + where, args.toArray());
        args.add(safeSize);
        args.add((safePage - 1) * safeSize);
        List<Map<String, Object>> list = jdbc.query("""
                select r.id, r.equipment_id, r.relate_id, r.name, r.type, r.content, r.status,
                       unix_timestamp(r.created_at) created_at, e.equipment_code, a.name archive_name,
                       coalesce(u.nickname, u.username, '') add_user
                from equipment_record r
                left join equipment_item e on e.id=r.equipment_id
                left join equipment_archive a on a.id=e.archive_id
                left join app_user u on u.id=r.add_user_id
                where %s
                order by r.id desc
                limit ? offset ?
                """.formatted(where), this::rowMap, args.toArray());
        return ApiResponse.ok(new PageResult<>(list, total, safePage, safeSize));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<Map<String, Object>> recordDetail(@PathVariable long id) {
        ready();
        Map<String, Object> row = one("""
                select r.*, unix_timestamp(r.created_at) created_at, e.equipment_code, a.name archive_name,
                       coalesce(u.nickname, u.username, '') add_user
                from equipment_record r
                left join equipment_item e on e.id=r.equipment_id
                left join equipment_archive a on a.id=e.archive_id
                left join app_user u on u.id=r.add_user_id
                where r.id=? and r.deleted_at is null
                """, id);
        if (row == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        return ApiResponse.ok(row);
    }

    @GetMapping("/repairs/{id}")
    public ApiResponse<Map<String, Object>> repairDetail(@PathVariable long id) {
        ready();
        Map<String, Object> row = one("""
                select r.*, unix_timestamp(r.register_time) register_time, unix_timestamp(r.assign_time) assign_time,
                       unix_timestamp(r.repair_time) repair_time, e.coding, e.equipment_code,
                       a.name archive_name, a.model archive_model, fc.name failure_cause,
                       coalesce(ru.nickname, ru.username, '') register_user,
                       coalesce(mu.nickname, mu.username, '') repair_user
                from repair_order r
                join equipment_item e on e.id=r.equipment_id
                join equipment_archive a on a.id=r.archive_id
                left join failure_cause fc on fc.id=r.failure_cause_id
                left join app_user ru on ru.id=r.register_user_id
                left join app_user mu on mu.id=r.repair_user_id
                where r.id=? and r.deleted_at is null
                """, id);
        if (row == null) {
            throw new IllegalArgumentException("维修工单不存在");
        }
        return ApiResponse.ok(row);
    }

    @PostMapping("/repairs/{id}/assignment")
    @Transactional
    public ApiResponse<Map<String, Object>> assignRepair(@PathVariable long id, @RequestBody Map<String, Object> body) {
        ready();
        long repairUserId = number(body.getOrDefault("repair_user_id", body.getOrDefault("user_id", 0)));
        if (repairUserId <= 0) {
            throw new IllegalArgumentException("请选择维修人员");
        }
        jdbc.update("update repair_order set repair_user_id=?, assign_time=now(), status='registered', updated_at=now() where id=? and deleted_at is null", repairUserId, id);
        return repairDetail(id);
    }

    @PostMapping("/repairs/{id}/register")
    @Transactional
    public ApiResponse<Map<String, Object>> registerRepair(@PathVariable long id, @RequestBody Map<String, Object> body) {
        ready();
        String status = string(body.getOrDefault("repair_status", body.getOrDefault("status", "repaired")));
        String target = "scrapped".equals(status) ? "scrapped" : "repaired";
        Map<String, Object> repair = one("select equipment_id, status from repair_order where id=? and deleted_at is null", id);
        if (repair == null) {
            throw new IllegalArgumentException("维修工单不存在");
        }
        long userId = number(body.getOrDefault("repair_user_id", body.getOrDefault("user_id", 0)));
        String content = string(body.getOrDefault("repair_content", body.getOrDefault("content", "")));
        String image = string(body.getOrDefault("repair_image", body.getOrDefault("image", "")));
        long failureCauseId = number(body.getOrDefault("failure_cause_id", 0));
        jdbc.update("""
                update repair_order
                set repair_user_id=case when ? > 0 then ? else repair_user_id end,
                    repair_time=now(), repair_content=?, repair_image=?, failure_cause_id=?, status=?, updated_at=now()
                where id=?
                """, userId, userId, content, image, failureCauseId, target, id);
        jdbc.update("update equipment_item set work_status=?, updated_at=now() where id=?",
                "scrapped".equals(target) ? "scrapped" : "normal", repair.get("equipment_id"));
        jdbc.update("""
                insert into equipment_record (equipment_id, relate_id, add_user_id, name, type, content, status, created_at, updated_at)
                values (?, ?, ?, ?, 'repair', ?, 'normal', now(), now())
                """, repair.get("equipment_id"), id, userId, "维修结果：" + ("scrapped".equals(target) ? "已报废" : "已修复"), content);
        return repairDetail(id);
    }

    private QueryParts listQuery(String resource, ResourceSpec spec, boolean recycle, String keyword) {
        List<Object> args = new ArrayList<>();
        String where = recycle ? "x.deleted_at is not null" : "x.deleted_at is null";
        if (keyword != null && !keyword.isBlank()) {
            String key = "%" + keyword + "%";
            switch (resource) {
                case "departments", "failure-causes" -> {
                    where += " and x.name like ?";
                    args.add(key);
                }
                case "suppliers" -> {
                    where += " and (x.name like ? or x.supplier_code like ? or x.contact like ?)";
                    args.add(key);
                    args.add(key);
                    args.add(key);
                }
                case "staff" -> {
                    where += " and (x.workno like ? or x.position like ? or exists(select 1 from app_user u where u.id=x.user_id and (u.nickname like ? or u.username like ?)))";
                    args.add(key);
                    args.add(key);
                    args.add(key);
                    args.add(key);
                }
                case "archives" -> {
                    where += " and (x.model like ? or x.name like ? or x.region like ?)";
                    args.add(key);
                    args.add(key);
                    args.add(key);
                }
                default -> {
                }
            }
        }
        String sql = switch (resource) {
            case "staff" -> """
                    select x.*, coalesce(u.nickname, u.username, '') nickname, coalesce(u.mobile, '') mobile, coalesce(d.name, '') department
                    from staff x
                    left join app_user u on u.id=x.user_id
                    left join department d on d.id=x.department_id
                    where %s
                    order by x.id desc
                    """.formatted(where);
            case "reminder-users" -> """
                    select x.*, coalesce(u.nickname, u.username, '') nickname, coalesce(d.name, '') department
                    from reminder_user x
                    left join staff s on s.id=x.staff_id
                    left join app_user u on u.id=s.user_id
                    left join department d on d.id=s.department_id
                    where %s
                    order by x.id desc
                    """.formatted(where);
            case "archives" -> """
                    select x.*, coalesce(s.name, '') supplier, coalesce(u.nickname, u.username, '') responsible
                    from equipment_archive x
                    left join supplier s on s.id=x.supplier_id
                    left join app_user u on u.id=x.responsible_user_id
                    where %s
                    order by x.id desc
                    """.formatted(where);
            default -> "select x.* from " + spec.table + " x where " + where + " order by x.id desc";
        };
        return new QueryParts(sql, where, args);
    }

    private int updateValues(String table, long id, Map<String, Object> values) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("update " + table + " set ");
        int index = 0;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (index++ > 0) {
                sql.append(", ");
            }
            sql.append(entry.getKey()).append("=?");
            args.add(entry.getValue());
        }
        sql.append(", updated_at=now() where id=?");
        args.add(id);
        return jdbc.update(sql.toString(), args.toArray());
    }

    private Map<String, Object> permittedValues(ResourceSpec spec, Map<String, Object> body) {
        Map<String, Object> values = new LinkedHashMap<>();
        spec.columns.forEach(column -> {
            if (body.containsKey(column)) {
                values.put(column, normalizeValue(column, body.get(column)));
            }
        });
        return values;
    }

    private Object normalizeValue(String column, Object value) {
        if (value == null || Objects.equals(value, "")) {
            return column.endsWith("_id") || "amount".equals(column) || "equipment_manage".equals(column) ? 0 : "";
        }
        if ("purchase_time".equals(column) && value instanceof Number number && number.longValue() > 0) {
            return Instant.ofEpochSecond(number.longValue()).atZone(ZONE).toLocalDateTime();
        }
        return value;
    }

    private void defaults(String resource, Map<String, Object> values) {
        values.putIfAbsent("status", "normal");
        if ("archives".equals(resource)) {
            values.putIfAbsent("amount", 0);
            values.putIfAbsent("model", "");
            values.putIfAbsent("name", "");
        }
    }

    private ResourceSpec spec(String resource) {
        ResourceSpec spec = RESOURCES.get(resource);
        if (spec == null) {
            throw new IllegalArgumentException("不支持的资源：" + resource);
        }
        return spec;
    }

    private void ready() {
        migrationService.ensureMigrated();
    }

    private Map<String, Object> getById(String table, long id) {
        Map<String, Object> row = one("select * from " + table + " where id=?", id);
        if (row == null) {
            throw new IllegalArgumentException("数据不存在");
        }
        return row;
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.query(sql, this::rowMap, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private long nextId(String table) {
        Long id = jdbc.queryForObject("select coalesce(max(id),0)+1 from " + table, Long.class);
        return id == null ? 1 : id;
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private long number(Object value) {
        if (value == null || Objects.equals(value, "")) {
            return 0;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> rowMap(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    private record ResourceSpec(String table, List<String> columns) {
    }

    private record QueryParts(String sql, String where, List<Object> args) {
    }
}
