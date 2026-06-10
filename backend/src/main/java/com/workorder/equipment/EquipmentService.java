package com.workorder.equipment;

import com.workorder.common.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.workorder.migration.LegacyMigrationService;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class EquipmentService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbc;
    private final LegacyMigrationService migrationService;

    public EquipmentService(JdbcTemplate jdbc, LegacyMigrationService migrationService) {
        this.jdbc = jdbc;
        this.migrationService = migrationService;
    }

    private void ready() {
        migrationService.ensureMigrated();
    }

    public EquipmentDtos.DashboardSummary summary() {
        ready();
        return new EquipmentDtos.DashboardSummary(
                count("select count(*) from equipment_archive where deleted_at is null"),
                count("select count(*) from equipment_item where deleted_at is null"),
                count("select count(*) from equipment_item where deleted_at is null and work_status='normal'"),
                count("select count(*) from equipment_item where deleted_at is null and work_status='repairing'"),
                count("select count(*) from equipment_item where deleted_at is null and work_status='scrapped'"),
                count("select count(*) from repair_order where deleted_at is null and status='pending'"),
                count("select count(*) from repair_order where deleted_at is null and status='registered'"),
                count("select count(*) from plan_task t join work_plan p on p.id=t.plan_id where t.deleted_at is null and p.type='inspection' and t.status='pending'"),
                count("select count(*) from plan_task t join work_plan p on p.id=t.plan_id where t.deleted_at is null and p.type='maintenance' and t.status='pending'")
        );
    }

    public PageResult<EquipmentDtos.ArchiveDto> archives(int page, int size) {
        ready();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        List<EquipmentDtos.ArchiveDto> records = jdbc.query("""
                        select a.id, a.model, a.name, a.amount, a.region,
                               coalesce(s.name, '') supplier_name,
                               coalesce(u.nickname, u.username, '') responsible_name
                        from equipment_archive a
                        left join supplier s on s.id=a.supplier_id
                        left join app_user u on u.id=a.responsible_user_id
                        where a.deleted_at is null
                        order by a.id desc
                        limit ? offset ?
                        """,
                this::archiveDto, safeSize, offset(safePage, safeSize));
        return new PageResult<>(records, count("select count(*) from equipment_archive where deleted_at is null"), safePage, safeSize);
    }

    public PageResult<EquipmentDtos.EquipmentItemDto> items(int page, int size, String status) {
        ready();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        boolean filtered = status != null && !status.isBlank();
        String where = "e.deleted_at is null" + (filtered ? " and e.work_status=?" : "");
        Object[] args = filtered
                ? new Object[]{status, safeSize, offset(safePage, safeSize)}
                : new Object[]{safeSize, offset(safePage, safeSize)};
        List<EquipmentDtos.EquipmentItemDto> records = jdbc.query("""
                        select e.id, e.archive_id, e.coding, e.equipment_code, e.work_status,
                               a.model archive_model, a.name archive_name, a.region
                        from equipment_item e
                        join equipment_archive a on a.id=e.archive_id
                        where %s
                        order by e.id desc
                        limit ? offset ?
                        """.formatted(where),
                this::itemDto, args);
        long total = filtered
                ? jdbc.queryForObject("select count(*) from equipment_item e where e.deleted_at is null and e.work_status=?", Long.class, status)
                : count("select count(*) from equipment_item e where e.deleted_at is null");
        return new PageResult<>(records, total, safePage, safeSize);
    }

    public EquipmentDtos.EquipmentDetailDto detail(Long id) {
        ready();
        EquipmentDtos.EquipmentItemDto item = jdbc.queryForObject("""
                        select e.id, e.archive_id, e.coding, e.equipment_code, e.work_status,
                               a.model archive_model, a.name archive_name, a.region
                        from equipment_item e
                        join equipment_archive a on a.id=e.archive_id
                        where e.id=?
                        """, this::itemDto, id);
        List<EquipmentDtos.RecordDto> records = jdbc.query("""
                        select r.id, r.equipment_id, r.relate_id, r.name, r.type, r.content, unix_timestamp(r.created_at) created_at,
                               coalesce(u.nickname, u.username, '') add_user
                        from equipment_record r
                        left join app_user u on u.id=r.add_user_id
                        where r.deleted_at is null and r.equipment_id=?
                        order by r.created_at desc
                        """, this::recordDto, id);
        Map<String, Object> todos = new HashMap<>();
        List<EquipmentDtos.RepairDto> repairs = jdbc.query(repairSql("r.equipment_id=? and r.status='registered'") + " limit 1", this::repairDto, id);
        if (!repairs.isEmpty()) {
            todos.put("repair", repairs.getFirst());
        }
        todos.put("tasks", jdbc.query(taskSql("t.equipment_id=? and t.status='pending'"), this::taskDto, id));
        return new EquipmentDtos.EquipmentDetailDto(item, records, todos);
    }

    public PageResult<EquipmentDtos.RepairDto> repairs(int page, int size, String type) {
        ready();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        String condition = repairCondition(type);
        List<EquipmentDtos.RepairDto> records = jdbc.query(repairSql(condition) + " limit ? offset ?", this::repairDto, safeSize, offset(safePage, safeSize));
        long total = count("select count(*) from repair_order r where r.deleted_at is null and " + condition);
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Transactional
    public EquipmentDtos.RepairDto createRepair(EquipmentDtos.CreateRepairRequest request) {
        ready();
        Map<String, Object> item = jdbc.queryForMap("select id, archive_id from equipment_item where id=? and deleted_at is null", request.equipmentId());
        long now = nowEpoch();
        String code = "R" + java.time.format.DateTimeFormatter.ofPattern("yyMMdd").format(LocalDateTime.now()) + "-" + String.format("%03d", count("select count(*) from repair_order where register_time>=from_unixtime(" + startOfDayEpoch() + ")") + 1);
        jdbc.update("""
                        insert into repair_order
                        (repair_code, archive_id, equipment_id, register_user_id, register_time, content, register_image, status, created_at, updated_at)
                        values (?, ?, ?, 0, from_unixtime(?), ?, ?, 'pending', from_unixtime(?), from_unixtime(?))
                        """,
                code, item.get("archive_id"), request.equipmentId(), now, request.content(), request.registerImage() == null ? "" : request.registerImage(), now, now);
        Long id = jdbc.queryForObject("select last_insert_id()", Long.class);
        jdbc.update("update equipment_item set work_status='repairing', updated_at=from_unixtime(?) where id=?", now, request.equipmentId());
        return jdbc.queryForObject(repairSql("r.id=?"), this::repairDto, id);
    }

    @Transactional
    public void receiveRepair(Long id) {
        ready();
        String status = jdbc.queryForObject("select status from repair_order where id=? and deleted_at is null", String.class, id);
        if (!"pending".equals(status)) {
            throw new IllegalArgumentException("只有待接单工单允许接收");
        }
        long now = nowEpoch();
        jdbc.update("update repair_order set repair_user_id=0, assign_time=from_unixtime(?), status='registered', updated_at=from_unixtime(?) where id=?", now, now, id);
    }

    @Transactional
    public void finishRepair(Long id, EquipmentDtos.FinishRepairRequest request) {
        ready();
        if (!"repaired".equals(request.repairStatus()) && !"scrapped".equals(request.repairStatus())) {
            throw new IllegalArgumentException("维修结果只能是已修复或已报废");
        }
        Map<String, Object> repair = jdbc.queryForMap("select equipment_id, status from repair_order where id=? and deleted_at is null", id);
        if (!"registered".equals(repair.get("status"))) {
            throw new IllegalArgumentException("只有维修中工单允许登记完成");
        }
        long now = nowEpoch();
        jdbc.update("""
                        update repair_order
                        set repair_time=from_unixtime(?), repair_content=?, repair_image=?, failure_cause_id=?, status=?, updated_at=from_unixtime(?)
                        where id=?
                        """,
                now, request.repairContent(), request.repairImage() == null ? "" : request.repairImage(),
                request.failureCauseId() == null ? 0 : request.failureCauseId(), request.repairStatus(), now, id);
        jdbc.update("update equipment_item set work_status=?, updated_at=from_unixtime(?) where id=?",
                "scrapped".equals(request.repairStatus()) ? "scrapped" : "normal", now, repair.get("equipment_id"));
        jdbc.update("""
                        insert into equipment_record (equipment_id, relate_id, add_user_id, name, type, content, status, created_at, updated_at)
                        values (?, ?, 0, ?, 'repair', ?, 'normal', from_unixtime(?), from_unixtime(?))
                        """,
                repair.get("equipment_id"), id, "维修结果：" + repairStatus(request.repairStatus()).text(), request.repairContent(), now, now);
    }

    public PageResult<EquipmentDtos.PlanTaskDto> planTasks(int page, int size, String type) {
        ready();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        boolean filtered = type != null && !type.isBlank();
        String condition = "t.deleted_at is null" + (filtered ? " and p.type=?" : "");
        Object[] args = filtered
                ? new Object[]{type, safeSize, offset(safePage, safeSize)}
                : new Object[]{safeSize, offset(safePage, safeSize)};
        List<EquipmentDtos.PlanTaskDto> records = jdbc.query(taskSql(condition) + " limit ? offset ?", this::taskDto, args);
        long total = filtered
                ? jdbc.queryForObject("select count(*) from plan_task t join work_plan p on p.id=t.plan_id where t.deleted_at is null and p.type=?", Long.class, type)
                : count("select count(*) from plan_task t where t.deleted_at is null");
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Transactional
    public void submitTask(Long id, EquipmentDtos.SubmitPlanTaskRequest request) {
        ready();
        Map<String, Object> task = jdbc.queryForMap("""
                select t.equipment_id, t.status, p.type
                from plan_task t join work_plan p on p.id=t.plan_id
                where t.id=? and t.deleted_at is null
                """, id);
        if (!"pending".equals(task.get("status"))) {
            throw new IllegalArgumentException("只有待处理任务允许提交");
        }
        long now = nowEpoch();
        Map<String, Object> content = request.content() == null ? Map.of() : request.content();
        jdbc.update("update plan_task set status='finish', updated_at=from_unixtime(?) where id=?", now, id);
        jdbc.update("""
                        insert into equipment_record (equipment_id, relate_id, add_user_id, name, type, content, status, created_at, updated_at)
                        values (?, ?, 0, ?, ?, ?, 'normal', from_unixtime(?), from_unixtime(?))
                        """,
                task.get("equipment_id"), id, "inspection".equals(task.get("type")) ? "巡检结果：" + content.getOrDefault("work_status", "已完成") : "保养完成",
                task.get("type"), String.valueOf(content), now, now);
    }

    private EquipmentDtos.ArchiveDto archiveDto(ResultSet rs, int rowNum) throws SQLException {
        return new EquipmentDtos.ArchiveDto(rs.getLong("id"), rs.getString("model"), rs.getString("name"), rs.getInt("amount"), rs.getString("region"), rs.getString("supplier_name"), rs.getString("responsible_name"));
    }

    private EquipmentDtos.EquipmentItemDto itemDto(ResultSet rs, int rowNum) throws SQLException {
        Enums.EquipmentWorkStatus status = workStatus(rs.getString("work_status"));
        return new EquipmentDtos.EquipmentItemDto(rs.getLong("id"), rs.getLong("archive_id"), rs.getString("coding"), rs.getString("equipment_code"),
                rs.getString("archive_model"), rs.getString("archive_name"), rs.getString("region"), status, status.text());
    }

    private EquipmentDtos.RepairDto repairDto(ResultSet rs, int rowNum) throws SQLException {
        Enums.RepairStatus status = repairStatus(rs.getString("status"));
        return new EquipmentDtos.RepairDto(rs.getLong("id"), rs.getString("repair_code"), rs.getLong("equipment_id"), rs.getString("equipment_code"),
                rs.getString("archive_model"), rs.getString("archive_name"), rs.getString("content"), rs.getString("register_user"),
                epoch(rs.getLong("register_time")), rs.getString("repair_user"), epoch(rs.getLong("assign_time")), epoch(rs.getLong("repair_time")), status, status.text());
    }

    private EquipmentDtos.PlanTaskDto taskDto(ResultSet rs, int rowNum) throws SQLException {
        Enums.PlanTaskStatus status = taskStatus(rs.getString("status"));
        return new EquipmentDtos.PlanTaskDto(rs.getLong("id"), rs.getString("coding"), rs.getString("type"), rs.getLong("equipment_id"),
                rs.getString("equipment_code"), rs.getString("archive_name"), rs.getString("task_user"),
                epoch(rs.getLong("start_time")), epoch(rs.getLong("due_time")), status, status.text());
    }

    private EquipmentDtos.RecordDto recordDto(ResultSet rs, int rowNum) throws SQLException {
        return new EquipmentDtos.RecordDto(rs.getLong("id"), rs.getLong("equipment_id"), rs.getLong("relate_id"), rs.getString("name"), rs.getString("type"), rs.getString("add_user"), rs.getString("content"), epoch(rs.getLong("created_at")));
    }

    private String repairSql(String condition) {
        return """
                select r.id, r.repair_code, r.equipment_id, r.content, r.status,
                       unix_timestamp(r.register_time) register_time,
                       unix_timestamp(r.assign_time) assign_time,
                       unix_timestamp(r.repair_time) repair_time,
                       e.equipment_code, a.model archive_model, a.name archive_name,
                       coalesce(ru.nickname, ru.username, '') register_user,
                       coalesce(mu.nickname, mu.username, '') repair_user
                from repair_order r
                join equipment_item e on e.id=r.equipment_id
                join equipment_archive a on a.id=r.archive_id
                left join app_user ru on ru.id=r.register_user_id
                left join app_user mu on mu.id=r.repair_user_id
                where r.deleted_at is null and %s
                order by r.register_time desc, r.id desc
                """.formatted(condition);
    }

    private String taskSql(String condition) {
        return """
                select t.id, t.coding, t.equipment_id, t.status,
                       unix_timestamp(t.start_time) start_time,
                       unix_timestamp(t.due_time) due_time,
                       p.type, e.equipment_code, a.name archive_name,
                       coalesce(u.nickname, u.username, '') task_user
                from plan_task t
                join work_plan p on p.id=t.plan_id
                join equipment_item e on e.id=t.equipment_id
                join equipment_archive a on a.id=e.archive_id
                left join app_user u on u.id=t.task_user_id
                where %s
                order by t.due_time asc, t.id desc
                """.formatted(condition);
    }

    private String repairCondition(String type) {
        return switch (type == null ? "" : type) {
            case "pending" -> "r.status='pending'";
            case "registered" -> "r.status='registered'";
            case "finish" -> "r.status in ('repaired','scrapped')";
            default -> "1=1";
        };
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private int offset(int page, int size) {
        return (page - 1) * size;
    }

    private long nowEpoch() {
        return Instant.now().getEpochSecond();
    }

    private long startOfDayEpoch() {
        return LocalDateTime.now(ZONE).toLocalDate().atStartOfDay(ZONE).toEpochSecond();
    }

    private LocalDateTime epoch(long seconds) {
        if (seconds <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZONE);
    }

    private Enums.EquipmentWorkStatus workStatus(String status) {
        try {
            return Enums.EquipmentWorkStatus.valueOf(status == null || status.isBlank() ? "normal" : status);
        } catch (IllegalArgumentException e) {
            return Enums.EquipmentWorkStatus.normal;
        }
    }

    private Enums.RepairStatus repairStatus(String status) {
        try {
            return Enums.RepairStatus.valueOf(status == null || status.isBlank() ? "pending" : status);
        } catch (IllegalArgumentException e) {
            return Enums.RepairStatus.pending;
        }
    }

    private Enums.PlanTaskStatus taskStatus(String status) {
        try {
            return Enums.PlanTaskStatus.valueOf(status == null || status.isBlank() ? "pending" : status);
        } catch (IllegalArgumentException e) {
            return Enums.PlanTaskStatus.pending;
        }
    }
}
