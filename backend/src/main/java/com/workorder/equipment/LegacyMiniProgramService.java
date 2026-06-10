package com.workorder.equipment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workorder.config.SystemConfigService;
import com.workorder.migration.LegacyMigrationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LegacyMiniProgramService {
    private final JdbcTemplate jdbc;
    private final SystemConfigService configService;
    private final LegacyMigrationService migrationService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public LegacyMiniProgramService(JdbcTemplate jdbc, SystemConfigService configService, LegacyMigrationService migrationService, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.configService = configService;
        this.migrationService = migrationService;
        this.objectMapper = objectMapper;
    }

    private void ready() {
        migrationService.ensureMigrated();
    }

    public Map<String, Object> systemInfo() {
        ready();
        Map<String, String> config = configService.values();
        return Map.of("manage_phone", config.getOrDefault("manage_phone", ""));
    }

    @Transactional
    public Map<String, Object> login(String mobile, String password, String code, String openid) {
        ready();
        if (isBlank(mobile) || isBlank(password)) {
            throw new IllegalArgumentException("参数不正确");
        }
        Map<String, Object> user = queryOne("select * from app_user where mobile=? and status='normal'", mobile);
        if (user == null || !matchesLegacyPassword(password, String.valueOf(user.get("salt")), String.valueOf(user.get("password_hash")))) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        Map<String, Object> staff = staffByUserId(number(user.get("id")));
        if (staff == null) {
            throw new IllegalArgumentException("该账号不存在或已被禁用");
        }
        String bindOpenid = resolveOpenid(code, openid);
        if (!isBlank(bindOpenid)) {
            jdbc.update("update staff set openid='', updated_at=now() where openid=?", bindOpenid);
            jdbc.update("update staff set openid=?, updated_at=now() where id=?", bindOpenid, staff.get("id"));
            staff.put("openid", bindOpenid);
        }
        return userInfo(user, staff);
    }

    public Map<String, Object> weappLogin(String code, String openid) {
        ready();
        if (isBlank(code) && isBlank(openid)) {
            throw new IllegalArgumentException("参数不正确");
        }
        String resolvedOpenid = resolveOpenid(code, openid);
        if (isBlank(resolvedOpenid)) {
            throw new IllegalArgumentException("微信登录失败，未获取到 openid");
        }
        Map<String, Object> staff = queryOne("select * from staff where openid=? and status='normal' and deleted_at is null", resolvedOpenid);
        if (staff == null) {
            throw new IllegalArgumentException("该微信号暂未绑定员工账号");
        }
        Map<String, Object> user = queryOne("select * from app_user where id=? and status='normal'", staff.get("user_id"));
        if (user == null) {
            throw new IllegalArgumentException("该账号已被禁用");
        }
        return userInfo(user, staff);
    }

    @Transactional
    public void logout(Long userId, String openid) {
        ready();
        if (userId != null && userId > 0) {
            jdbc.update("update staff set openid='', updated_at=now() where user_id=?", userId);
            return;
        }
        if (!isBlank(openid)) {
            jdbc.update("update staff set openid='', updated_at=now() where openid=?", openid);
        }
    }

    public Map<String, Object> staffInfo(Long userId, String openid) {
        ready();
        Map<String, Object> staff = staff(userId, openid);
        Map<String, Object> user = queryOne("select * from app_user where id=?", staff.get("user_id"));
        Map<String, Object> department = queryOne("select * from department where id=?", staff.get("department_id"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.get("id"));
        result.put("nickname", user.get("nickname"));
        result.put("mobile", user.get("mobile"));
        result.put("avatar", user.get("avatar"));
        result.put("workno", staff.get("workno"));
        result.put("position", staff.get("position"));
        result.put("department", department == null ? "" : department.get("name"));
        return result;
    }

    public Map<String, Object> workbench(Long userId, String openid) {
        ready();
        long uid = authUserId(userId, openid);
        long now = Instant.now().getEpochSecond();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("repair", count("select count(*) from repair_order where status='registered' and repair_user_id=? and deleted_at is null", uid));
        result.put("repairPool", count("select count(*) from repair_order where status='pending' and deleted_at is null"));
        result.put("inspection", countActiveTasks(uid, "inspection", now));
        result.put("maintenance", countActiveTasks(uid, "maintenance", now));
        result.put("statisticChart", Map.of(
                "equipmentStatus", List.of(
                        Map.of("name", "正常运行 " + count("select count(*) from equipment_item where work_status='normal' and deleted_at is null") + "台", "value", count("select count(*) from equipment_item where work_status='normal' and deleted_at is null")),
                        Map.of("name", "停机待修 " + count("select count(*) from equipment_item where work_status='repairing' and deleted_at is null") + "台", "value", count("select count(*) from equipment_item where work_status='repairing' and deleted_at is null")),
                        Map.of("name", "报废停用 " + count("select count(*) from equipment_item where work_status='scrapped' and deleted_at is null") + "台", "value", count("select count(*) from equipment_item where work_status='scrapped' and deleted_at is null"))
                )
        ));
        return result;
    }

    public Map<String, Object> archives(int page, int pageSize) {
        ready();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        long total = count("select count(*) from equipment_archive where status='normal' and deleted_at is null");
        List<Map<String, Object>> list = jdbc.query("""
                select a.id, a.name, a.model, a.amount, a.region, coalesce(s.name, '') supplier
                from equipment_archive a
                left join supplier s on s.id=a.supplier_id
                where a.status='normal' and a.deleted_at is null
                order by a.id desc
                limit ? offset ?
                """, this::rowMap, safeSize, offset(safePage, safeSize));
        return page(list, total, safePage, safeSize);
    }

    public Map<String, Object> equipments(String status, String archiveKeyword, String equipmentKeyword, String planType, Long archiveId, int page, int pageSize) {
        ready();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("e.status='normal' and e.deleted_at is null");
        if ("scrapped".equals(status)) {
            where.append(" and e.work_status='scrapped'");
        } else {
            where.append(" and e.work_status in ('normal','repairing')");
        }
        if (archiveId != null && archiveId > 0) {
            where.append(" and a.id=?");
            args.add(archiveId);
        }
        if (!isBlank(archiveKeyword)) {
            where.append(" and (a.model like ? or a.name like ?)");
            args.add("%" + archiveKeyword + "%");
            args.add("%" + archiveKeyword + "%");
        }
        if (!isBlank(equipmentKeyword)) {
            where.append(" and e.equipment_code like ?");
            args.add("%" + equipmentKeyword + "%");
        }
        if (!isBlank(planType)) {
            where.append(" and exists (select 1 from plan_task t join work_plan p on p.id=t.plan_id where t.equipment_id=e.id and t.status='pending' and p.type=?)");
            args.add(planType);
        }
        long total = jdbc.queryForObject("select count(*) from equipment_item e join equipment_archive a on a.id=e.archive_id where " + where, Long.class, args.toArray());
        args.add(safeSize);
        args.add(offset(safePage, safeSize));
        List<Map<String, Object>> list = jdbc.query("""
                select a.id archive_id, a.name, a.model, a.region, e.id equipment_id, e.coding, e.equipment_code,
                       e.work_status, e.work_status work_status_text
                from equipment_item e
                join equipment_archive a on a.id=e.archive_id
                where %s
                order by e.id desc
                limit ? offset ?
                """.formatted(where), this::rowMap, args.toArray());
        list.forEach(row -> row.put("work_status_text", workStatusText(String.valueOf(row.get("work_status")))));
        return page(list, total, safePage, safeSize);
    }

    public Map<String, Object> equipmentInfo(String coding, String type) {
        ready();
        if (isBlank(coding)) {
            throw new IllegalArgumentException("未知设备");
        }
        Map<String, Object> info = queryOne("""
                select e.id, e.coding, e.equipment_code, e.work_status, a.model archive_model, a.name archive_name,
                       a.region, a.remark, a.document, unix_timestamp(a.purchase_time) purchasetime, coalesce(s.name, '') supplier,
                       coalesce(u.nickname, u.username, '') responsible_name, coalesce(u.mobile, '') responsible_mobile
                from equipment_item e
                join equipment_archive a on a.id=e.archive_id
                left join supplier s on s.id=a.supplier_id
                left join app_user u on u.id=a.responsible_user_id
                where e.coding=? and e.deleted_at is null
                """, coding);
        if (info == null) {
            throw new IllegalArgumentException("未知设备");
        }
        info.put("work_status_text", workStatusText(String.valueOf(info.get("work_status"))));
        info.put("todos", equipmentTodos(number(info.get("id")), type));
        info.put("records", jdbc.query("""
                select r.id, r.name, case when r.add_user_id=0 then '系统管理员' else coalesce(u.nickname, u.username, '') end user,
                       r.type, date_format(r.created_at, '%Y年%m月%d日 %H:%i:%s') createtime
                from equipment_record r
                left join app_user u on u.id=r.add_user_id
                where r.equipment_id=? and r.deleted_at is null
                order by r.id desc
                """, this::rowMap, info.get("id")));
        return info;
    }

    @Transactional
    public void createRepair(Long equipmentId, String content, String registerImage, Long userId, String openid) {
        ready();
        long uid = authUserId(userId, openid);
        Map<String, Object> equipment = queryOne("select id, archive_id from equipment_item where id=? and status='normal' and deleted_at is null", equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("未知设备");
        }
        if (count("select count(*) from repair_order where equipment_id=? and status in ('pending','registered') and deleted_at is null", equipmentId) > 0) {
            throw new IllegalArgumentException("该设备正在维修中，无需重复提交");
        }
        long now = Instant.now().getEpochSecond();
        String code = "R" + java.time.format.DateTimeFormatter.ofPattern("yyMMdd").withZone(java.time.ZoneId.of("Asia/Shanghai")).format(Instant.now()) + "-" + String.format("%03d", count("select count(*) from repair_order where register_time>=curdate()") + 1);
        jdbc.update("""
                insert into repair_order
                (repair_code, archive_id, equipment_id, register_user_id, register_time, content, register_image, status, created_at, updated_at)
                values (?, ?, ?, ?, from_unixtime(?), ?, ?, 'pending', from_unixtime(?), from_unixtime(?))
                """, code, equipment.get("archive_id"), equipmentId, uid, now, clean(content), clean(registerImage), now, now);
        jdbc.update("update equipment_item set work_status='repairing', updated_at=from_unixtime(?) where id=?", now, equipmentId);
    }

    public List<Map<String, Object>> failureCauses() {
        ready();
        return jdbc.query("select id, name from failure_cause where status='normal' and deleted_at is null order by id", this::rowMap);
    }

    public List<Map<String, Object>> planTaskFields(Long id) {
        ready();
        Map<String, Object> task = queryOne("select plan_id from plan_task where id=?", id);
        if (task == null) {
            throw new IllegalArgumentException("未知计划任务");
        }
        return jdbc.query("select * from plan_field where plan_id=? and deleted_at is null order by sort asc, id asc", this::rowMap, task.get("plan_id"));
    }

    @Transactional
    public void submitPlanTask(Long id, String type, Map<String, Object> content, Long userId, String openid) {
        ready();
        long uid = authUserId(userId, openid);
        Map<String, Object> task = queryOne("""
                select t.equipment_id, t.status, p.type
                from plan_task t
                join work_plan p on p.id=t.plan_id
                where t.id=? and t.deleted_at is null
                """, id);
        if (task == null) {
            throw new IllegalArgumentException("未知计划任务");
        }
        if (!"pending".equals(task.get("status"))) {
            throw new IllegalArgumentException("该任务已处理");
        }
        String taskType = isBlank(type) ? String.valueOf(task.get("type")) : type;
        String contentText = content == null ? "{}" : content.toString();
        String name = "maintenance".equals(taskType) ? "保养完成" : "巡检结果：" + (content == null ? "已完成" : content.getOrDefault("work_status", "已完成"));
        long now = Instant.now().getEpochSecond();
        jdbc.update("update plan_task set task_user_id=?, status='finish', updated_at=from_unixtime(?) where id=?", uid, now, id);
        jdbc.update("""
                insert into equipment_record (equipment_id, relate_id, add_user_id, name, type, content, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, 'normal', from_unixtime(?), from_unixtime(?))
                """, task.get("equipment_id"), id, uid, name, taskType, contentText, now, now);
    }

    public Map<String, Object> repairs(Long userId, String openid, String type, int page, int pageSize) {
        ready();
        long uid = authUserId(userId, openid);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("r.deleted_at is null");
        if ("receive".equals(type) || "pool".equals(type) || "pending".equals(type)) {
            where.append(" and r.status='pending'");
        } else if ("finish".equals(type)) {
            where.append(" and r.status in ('repaired','scrapped')");
        } else {
            where.append(" and r.status='registered' and r.repair_user_id=?");
            args.add(uid);
        }
        long total = jdbc.queryForObject("select count(*) from repair_order r where " + where, Long.class, args.toArray());
        args.add(safeSize);
        args.add(offset(safePage, safeSize));
        List<Map<String, Object>> list = jdbc.query("""
                select r.id, r.repair_code, r.equipment_id, r.content, r.register_image, r.repair_content, r.repair_image,
                       r.status, r.status status_text, unix_timestamp(r.register_time) registertime,
                       unix_timestamp(r.assign_time) assigntime, unix_timestamp(r.repair_time) repairtime,
                       e.coding, e.equipment_code, a.name archive_name, a.model archive_model,
                       coalesce(ru.nickname, ru.username, '') register_user,
                       coalesce(mu.nickname, mu.username, '') repair_user
                from repair_order r
                join equipment_item e on e.id=r.equipment_id
                join equipment_archive a on a.id=r.archive_id
                left join app_user ru on ru.id=r.register_user_id
                left join app_user mu on mu.id=r.repair_user_id
                where %s
                order by r.register_time desc, r.id desc
                limit ? offset ?
                """.formatted(where), this::rowMap, args.toArray());
        list.forEach(row -> row.put("status_text", repairStatusText(String.valueOf(row.get("status")))));
        return page(list, total, safePage, safeSize);
    }

    @Transactional
    public void receiveRepair(Long id, Long userId, String openid) {
        ready();
        long uid = authUserId(userId, openid);
        Map<String, Object> repair = queryOne("select status from repair_order where id=? and deleted_at is null", id);
        if (repair == null) {
            throw new IllegalArgumentException("未知维修工单");
        }
        if (!"pending".equals(repair.get("status"))) {
            throw new IllegalArgumentException("该维修工单不能接单");
        }
        jdbc.update("update repair_order set repair_user_id=?, assign_time=now(), status='registered', updated_at=now() where id=?", uid, id);
    }

    public Map<String, Object> repairInfo(Long id) {
        ready();
        Map<String, Object> info = queryOne("""
                select r.id, r.repair_code, r.equipment_id, r.content, r.register_image, r.repair_content, r.repair_image,
                       r.failure_cause_id, fc.name failure_cause, r.status, r.status status_text,
                       unix_timestamp(r.register_time) registertime, unix_timestamp(r.assign_time) assigntime,
                       unix_timestamp(r.repair_time) repairtime, e.coding, e.equipment_code,
                       a.name archive_name, a.model archive_model,
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
        if (info == null) {
            throw new IllegalArgumentException("未知维修工单");
        }
        info.put("status_text", repairStatusText(String.valueOf(info.get("status"))));
        return info;
    }

    @Transactional
    public void registerRepair(Long id, String repairStatus, String content, String image, Long failureCauseId, Long userId, String openid) {
        ready();
        long uid = authUserId(userId, openid);
        String target = "scrapped".equals(repairStatus) ? "scrapped" : "repaired";
        Map<String, Object> repair = queryOne("select equipment_id, status from repair_order where id=? and deleted_at is null", id);
        if (repair == null) {
            throw new IllegalArgumentException("未知维修工单");
        }
        if (!"registered".equals(repair.get("status"))) {
            throw new IllegalArgumentException("该维修工单不能登记");
        }
        jdbc.update("""
                update repair_order
                set repair_user_id=?, repair_time=now(), repair_content=?, repair_image=?, failure_cause_id=?, status=?, updated_at=now()
                where id=?
                """, uid, clean(content), clean(image), failureCauseId == null ? 0 : failureCauseId, target, id);
        jdbc.update("update equipment_item set work_status=?, updated_at=now() where id=?",
                "scrapped".equals(target) ? "scrapped" : "normal", repair.get("equipment_id"));
        jdbc.update("""
                insert into equipment_record (equipment_id, relate_id, add_user_id, name, type, content, status, created_at, updated_at)
                values (?, ?, ?, ?, 'repair', ?, 'normal', now(), now())
                """, repair.get("equipment_id"), id, uid, "维修结果：" + repairStatusText(target), clean(content));
    }

    public Map<String, Object> recordInfo(Long id) {
        ready();
        Map<String, Object> record = queryOne("""
                select r.*, unix_timestamp(r.created_at) createtime, coalesce(u.nickname, u.username, '') user
                from equipment_record r
                left join app_user u on u.id=r.add_user_id
                where r.id=? and r.deleted_at is null
                """, id);
        if (record == null) {
            throw new IllegalArgumentException("未知记录");
        }
        return record;
    }

    public Map<String, Object> qrcode(String coding) {
        ready();
        if (isBlank(coding)) {
            throw new IllegalArgumentException("未知设备");
        }
        Map<String, String> config = configService.values();
        String domain = config.getOrDefault("qrcode_domain", "");
        String path = "/pages/equipment/info?coding=" + coding;
        return Map.of("coding", coding, "path", path, "url", isBlank(domain) ? path : domain.replaceAll("/+$", "") + path);
    }

    public List<Map<String, Object>> relationshipList() {
        ready();
        return jdbc.query("""
                select s.id staff_id, s.user_id, s.workno, s.position, s.openid,
                       coalesce(u.nickname, u.username, '') nickname, u.mobile, d.name department
                from staff s
                left join app_user u on u.id=s.user_id
                left join department d on d.id=s.department_id
                where s.deleted_at is null and s.status='normal'
                order by s.id asc
                """, this::rowMap);
    }

    public void unbind(Long userId, String openid) {
        logout(userId, openid);
    }

    private Map<String, Object> userInfo(Map<String, Object> user, Map<String, Object> staff) {
        Map<String, Object> department = queryOne("select * from department where id=?", staff.get("department_id"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.get("id"));
        data.put("username", user.get("username"));
        data.put("nickname", user.get("nickname"));
        data.put("mobile", user.get("mobile"));
        data.put("avatar", user.get("avatar"));
        data.put("token", "");
        data.put("openid", staff.get("openid"));
        data.put("staff_id", staff.get("id"));
        data.put("equipment_manage", department == null ? 0 : department.get("equipment_manage"));
        return data;
    }

    private Map<String, Object> staff(Long userId, String openid) {
        Map<String, Object> staff = userId != null && userId > 0 ? staffByUserId(userId) : null;
        if (staff == null && !isBlank(openid)) {
            staff = queryOne("select * from staff where openid=? and status='normal' and deleted_at is null", openid);
        }
        if (staff == null) {
            throw new IllegalArgumentException("该账号不存在或已被禁用");
        }
        return staff;
    }

    private Map<String, Object> staffByUserId(long userId) {
        return queryOne("select * from staff where user_id=? and status='normal' and deleted_at is null", userId);
    }

    private long authUserId(Long userId, String openid) {
        return number(staff(userId, openid).get("user_id"));
    }

    private long countActiveTasks(long uid, String type, long now) {
        return count("""
                select count(*)
                from plan_task t
                join work_plan p on p.id=t.plan_id
                join plan_user pu on pu.plan_id=p.id
                join equipment_item e on e.id=t.equipment_id
                where p.type=? and pu.user_id=? and t.status='pending' and e.work_status<>'scrapped'
                  and p.last_due_time>=from_unixtime(?) and t.start_time<=from_unixtime(?) and t.due_time>=from_unixtime(?) and t.deleted_at is null and p.deleted_at is null
                """, type, uid, now, now, now);
    }

    private Map<String, Object> equipmentTodos(long equipmentId, String type) {
        Map<String, Object> todos = new LinkedHashMap<>();
        List<Map<String, Object>> repairs = jdbc.query("""
                select id, repair_code, content, status, unix_timestamp(register_time) registertime
                from repair_order
                where equipment_id=? and status in ('pending','registered') and deleted_at is null
                order by id desc limit 1
                """, this::rowMap, equipmentId);
        if (!repairs.isEmpty()) {
            todos.put("repair", repairs.getFirst());
        }
        List<Object> args = new ArrayList<>();
        args.add(equipmentId);
        String condition = "";
        if (!isBlank(type)) {
            condition = " and p.type=?";
            args.add(type);
        }
        todos.put("tasks", jdbc.query("""
                select t.id, t.coding, p.type, t.status, unix_timestamp(t.start_time) starttime, unix_timestamp(t.due_time) duetime
                from plan_task t
                join work_plan p on p.id=t.plan_id
                where t.equipment_id=? and t.status='pending' and t.deleted_at is null%s
                order by t.due_time asc
                """.formatted(condition), this::rowMap, args.toArray()));
        return todos;
    }

    private Map<String, Object> queryOne(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.query(sql, this::rowMap, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private String resolveOpenid(String code, String openid) {
        if (!isBlank(openid)) {
            return openid;
        }
        if (isBlank(code)) {
            return "";
        }
        if (code.startsWith("openid:")) {
            return code.substring("openid:".length());
        }
        Map<String, String> config = configService.values();
        String appId = config.getOrDefault("weappid", "");
        String secret = config.getOrDefault("weappsecret", "");
        if (isBlank(appId) || isBlank(secret)) {
            throw new IllegalArgumentException("小程序 AppID/Secret 未配置");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + encode(appId)
                + "&secret=" + encode(secret)
                + "&js_code=" + encode(code)
                + "&grant_type=authorization_code";
        try {
            String body = restTemplate.getForObject(url, String.class);
            Map<String, Object> data = objectMapper.readValue(body == null ? "{}" : body, new TypeReference<>() {});
            Object errorCode = data.get("errcode");
            if (errorCode != null && !"0".equals(String.valueOf(errorCode))) {
                throw new IllegalArgumentException("微信登录失败：" + data.getOrDefault("errmsg", errorCode));
            }
            Object resolved = data.get("openid");
            return resolved == null ? "" : String.valueOf(resolved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RestClientException e) {
            throw new IllegalArgumentException("微信登录服务暂不可用");
        } catch (Exception e) {
            throw new IllegalArgumentException("微信登录响应解析失败");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Map<String, Object> rowMap(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    private Map<String, Object> page(List<Map<String, Object>> list, long total, int page, int pageSize) {
        return Map.of("list", list, "count", list.size(), "total_count", total, "current_page", page, "total_page", Math.max(1, (long) Math.ceil(total * 1.0 / pageSize)));
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private int offset(int page, int size) {
        return (page - 1) * size;
    }

    private boolean matchesLegacyPassword(String raw, String salt, String hashed) {
        return md5(md5(raw) + salt).equalsIgnoreCase(hashed);
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    private String workStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "repairing" -> "停机待修";
            case "scrapped" -> "报废停用";
            default -> "正常运行";
        };
    }

    private String repairStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "pending" -> "待接单";
            case "registered" -> "维修中";
            case "scrapped" -> "已报废";
            case "repaired" -> "已修复";
            default -> status == null ? "" : status;
        };
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
