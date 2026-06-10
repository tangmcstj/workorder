package com.workorder.migration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class LegacyMigrationService {
    private final JdbcTemplate jdbc;
    private final AtomicBoolean checked = new AtomicBoolean(false);

    public LegacyMigrationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureMigrated() {
        if (checked.compareAndSet(false, true)) {
            ensureSchema();
            if (count("select count(*) from equipment_item") == 0 && tableExists("fa_equipment_equipment")) {
                migrate();
            }
        }
    }

    @Transactional
    public Map<String, Object> migrate() {
        ensureSchema();
        migrateUsers();
        migrateBaseData();
        migrateEquipment();
        migratePlans();
        migrateBusiness();
        return report();
    }

    public Map<String, Object> report() {
        ensureSchema();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tables", List.of(
                pair("fa_user", "app_user"),
                pair("fa_equipment_department", "department"),
                pair("fa_equipment_staff", "staff"),
                pair("fa_equipment_supplier", "supplier"),
                pair("fa_equipment_failure_cause", "failure_cause"),
                pair("fa_equipment_archive", "equipment_archive"),
                pair("fa_equipment_equipment", "equipment_item"),
                pair("fa_equipment_plan", "work_plan"),
                pair("fa_equipment_plan_task", "plan_task"),
                pair("fa_equipment_repair", "repair_order"),
                pair("fa_equipment_record", "equipment_record")
        ));
        report.put("status", Map.of(
                "equipmentWorkStatus", queryCounts("select work_status status, count(*) count from equipment_item group by work_status"),
                "repairStatus", queryCounts("select status, count(*) count from repair_order group by status"),
                "taskStatus", queryCounts("select status, count(*) count from plan_task group by status")
        ));
        return report;
    }

    private Map<String, Object> pair(String legacyTable, String newTable) {
        return Map.of(
                "legacyTable", legacyTable,
                "legacyCount", tableExists(legacyTable) ? count("select count(*) from " + legacyTable) : 0,
                "newTable", newTable,
                "newCount", count("select count(*) from " + newTable)
        );
    }

    private void migrateUsers() {
        jdbc.update("""
                insert into app_user (id, legacy_id, username, nickname, password_hash, salt, mobile, avatar, status, created_at, updated_at)
                select id, id, coalesce(username, ''), coalesce(nickname, username, ''), coalesce(password, ''), coalesce(salt, ''),
                       coalesce(mobile, ''), coalesce(avatar, ''), coalesce(status, 'normal'), from_unixtime(coalesce(createtime, unix_timestamp())),
                       if(updatetime is null, null, from_unixtime(updatetime))
                from fa_user
                on duplicate key update username=values(username), nickname=values(nickname), mobile=values(mobile), avatar=values(avatar),
                  status=values(status), updated_at=values(updated_at)
                """);
    }

    private void migrateBaseData() {
        jdbc.update("""
                insert into department (id, legacy_id, name, equipment_manage, status, created_at, updated_at, deleted_at)
                select id, id, name, equipment_manage, status, from_unixtime(coalesce(createtime, unix_timestamp())),
                       if(updatetime is null, null, from_unixtime(updatetime)), if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_department
                on duplicate key update name=values(name), equipment_manage=values(equipment_manage), status=values(status),
                  updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
        jdbc.update("""
                insert into staff (id, legacy_id, user_id, department_id, workno, position, openid, status, created_at, updated_at, deleted_at)
                select id, id, user_id, department_id, coalesce(workno, ''), coalesce(position, ''), coalesce(openid, ''), status,
                       from_unixtime(coalesce(createtime, unix_timestamp())), if(updatetime is null, null, from_unixtime(updatetime)),
                       if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_staff
                on duplicate key update user_id=values(user_id), department_id=values(department_id), workno=values(workno),
                  position=values(position), openid=values(openid), status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
        jdbc.update("""
                insert into supplier (id, legacy_id, supplier_code, name, relationship, bank, bank_account, contact, contact_mobile, remark, status, created_at, updated_at, deleted_at)
                select id, id, coalesce(supplier_code, ''), name, coalesce(relationship, ''), coalesce(bank, ''), coalesce(bank_account, ''),
                       coalesce(contact, ''), coalesce(contact_mobile, ''), coalesce(remark, ''), status,
                       from_unixtime(coalesce(createtime, unix_timestamp())), if(updatetime is null, null, from_unixtime(updatetime)),
                       if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_supplier
                on duplicate key update name=values(name), contact=values(contact), contact_mobile=values(contact_mobile),
                  remark=values(remark), status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
        jdbc.update("""
                insert into failure_cause (id, legacy_id, name, status, created_at, updated_at, deleted_at)
                select id, id, name, status, from_unixtime(coalesce(createtime, unix_timestamp())),
                       if(updatetime is null, null, from_unixtime(updatetime)), if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_failure_cause
                on duplicate key update name=values(name), status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
    }

    private void migrateEquipment() {
        jdbc.update("""
                insert into equipment_archive (id, legacy_id, model, name, parameter, amount, supplier_id, purchase_time, region,
                  responsible_user_id, document, remark, status, created_at, updated_at, deleted_at)
                select id, id, model, name, coalesce(parameter, ''), amount, supplier_id, if(purchasetime is null, null, from_unixtime(purchasetime)),
                       region, responsible_uid, coalesce(document, ''), coalesce(remark, ''), status,
                       from_unixtime(coalesce(createtime, unix_timestamp())), if(updatetime is null, null, from_unixtime(updatetime)),
                       if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_archive
                on duplicate key update model=values(model), name=values(name), amount=values(amount), supplier_id=values(supplier_id),
                  region=values(region), responsible_user_id=values(responsible_user_id), document=values(document), remark=values(remark),
                  status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
        jdbc.update("""
                insert into equipment_item (id, legacy_id, archive_id, coding, equipment_code, work_status, status, created_at, updated_at, deleted_at)
                select id, id, archive_id, coding, equipment_code, work_status, status, from_unixtime(coalesce(createtime, unix_timestamp())),
                       if(updatetime is null, null, from_unixtime(updatetime)), if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_equipment
                on duplicate key update archive_id=values(archive_id), coding=values(coding), equipment_code=values(equipment_code),
                  work_status=values(work_status), status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
    }

    private void migratePlans() {
        jdbc.update("""
                insert into work_plan (id, legacy_id, coding, name, type, periodicity, first_due_time, last_due_time, status, created_at, updated_at, deleted_at)
                select id, id, coding, name, type, periodicity, if(first_duetime is null, null, from_unixtime(first_duetime)),
                       if(last_duetime is null, null, from_unixtime(last_duetime)), status, from_unixtime(coalesce(createtime, unix_timestamp())),
                       if(updatetime is null, null, from_unixtime(updatetime)), if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_plan
                on duplicate key update name=values(name), type=values(type), periodicity=values(periodicity), last_due_time=values(last_due_time),
                  status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
        jdbc.update("""
                insert into plan_field (id, legacy_id, plan_id, label, name, type, default_value, options, attributes, required, sort, status, created_at, updated_at, deleted_at)
                select id, id, plan_id, label, name, type, coalesce(`default`, ''), coalesce(options, ''), coalesce(attributes, ''), `require`, sort, status,
                       from_unixtime(coalesce(createtime, unix_timestamp())), if(updatetime is null, null, from_unixtime(updatetime)),
                       if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_plan_field
                on duplicate key update label=values(label), name=values(name), type=values(type), default_value=values(default_value),
                  options=values(options), attributes=values(attributes), required=values(required), sort=values(sort), status=values(status)
                """);
        jdbc.update("replace into plan_user (plan_id, user_id) select plan_id, user_id from fa_equipment_plan_user");
        jdbc.update("replace into plan_archive (plan_id, archive_id) select plan_id, archive_id from fa_equipment_plan_archive");
    }

    private void migrateBusiness() {
        jdbc.update("""
                insert into plan_task (id, legacy_id, coding, plan_id, equipment_id, task_user_id, type, status, start_time, due_time, created_at, updated_at, deleted_at)
                select t.id, t.id, t.coding, t.plan_id, t.equipment_id, t.task_uid, p.type, t.status,
                       if(t.starttime is null, null, from_unixtime(t.starttime)), if(t.duetime is null, null, from_unixtime(t.duetime)),
                       from_unixtime(coalesce(t.createtime, unix_timestamp())), if(t.updatetime is null, null, from_unixtime(t.updatetime)),
                       if(t.deletetime is null, null, from_unixtime(t.deletetime))
                from fa_equipment_plan_task t join fa_equipment_plan p on p.id=t.plan_id
                on duplicate key update task_user_id=values(task_user_id), status=values(status), start_time=values(start_time), due_time=values(due_time),
                  updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
        jdbc.update("""
                insert into repair_order (id, legacy_id, repair_code, archive_id, equipment_id, register_user_id, register_time, content, register_image,
                  repair_user_id, assign_time, repair_time, repair_content, repair_image, failure_cause_id, consuming_seconds, status, created_at, updated_at, deleted_at)
                select id, id, repair_code, archive_id, equipment_id, register_uid, if(registertime is null, null, from_unixtime(registertime)),
                       coalesce(content, ''), coalesce(register_image, ''), repair_uid, if(assigntime is null, null, from_unixtime(assigntime)),
                       if(repairtime is null, null, from_unixtime(repairtime)), coalesce(repair_content, ''), coalesce(repair_image, ''),
                       failure_cause_id, coalesce(consuming, 0), status, from_unixtime(coalesce(createtime, unix_timestamp())),
                       if(updatetime is null, null, from_unixtime(updatetime)), if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_repair
                on duplicate key update repair_user_id=values(repair_user_id), assign_time=values(assign_time), repair_time=values(repair_time),
                  repair_content=values(repair_content), repair_image=values(repair_image), failure_cause_id=values(failure_cause_id),
                  consuming_seconds=values(consuming_seconds), status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
        jdbc.update("""
                insert into equipment_record (id, legacy_id, equipment_id, relate_id, add_user_id, name, type, content, status, created_at, updated_at, deleted_at)
                select id, id, equipment_id, relate_id, add_uid, name, type, coalesce(content, ''), status,
                       from_unixtime(coalesce(createtime, unix_timestamp())), if(updatetime is null, null, from_unixtime(updatetime)),
                       if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_record
                on duplicate key update name=values(name), content=values(content), status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
        jdbc.update("""
                insert into reminder_user (id, legacy_id, staff_id, type, status, created_at, updated_at, deleted_at)
                select id, id, staff_id, type, status, from_unixtime(coalesce(createtime, unix_timestamp())),
                       if(updatetime is null, null, from_unixtime(updatetime)), if(deletetime is null, null, from_unixtime(deletetime))
                from fa_equipment_reminder_users
                on duplicate key update staff_id=values(staff_id), type=values(type), status=values(status), updated_at=values(updated_at), deleted_at=values(deleted_at)
                """);
    }

    private void ensureSchema() {
        jdbc.execute("create table if not exists app_user (id bigint primary key, legacy_id bigint unique, username varchar(64), nickname varchar(100), password_hash varchar(64), salt varchar(30), mobile varchar(20), avatar varchar(500), status varchar(30), created_at datetime, updated_at datetime null)");
        jdbc.execute("create table if not exists department (id bigint primary key, legacy_id bigint unique, name varchar(255), equipment_manage tinyint(1), status varchar(30), created_at datetime, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists staff (id bigint primary key, legacy_id bigint unique, user_id bigint, department_id bigint, workno varchar(50), position varchar(255), openid varchar(100), status varchar(30), created_at datetime, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists supplier (id bigint primary key, legacy_id bigint unique, supplier_code varchar(50), name varchar(255), relationship varchar(255), bank varchar(255), bank_account varchar(255), contact varchar(100), contact_mobile varchar(30), remark text, status varchar(30), created_at datetime, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists failure_cause (id bigint primary key, legacy_id bigint unique, name varchar(255), status varchar(30), created_at datetime, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists work_plan (id bigint primary key, legacy_id bigint unique, coding varchar(30), name varchar(255), type varchar(30), periodicity varchar(50), first_due_time datetime null, last_due_time datetime null, status varchar(30), created_at datetime, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists plan_field (id bigint primary key, legacy_id bigint unique, plan_id bigint, label varchar(255), name varchar(255), type varchar(50), default_value text, options text, attributes text, required tinyint(1), sort int, status varchar(30), created_at datetime, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists plan_user (plan_id bigint, user_id bigint, primary key(plan_id, user_id))");
        jdbc.execute("create table if not exists plan_archive (plan_id bigint, archive_id bigint, primary key(plan_id, archive_id))");
        jdbc.execute("create table if not exists reminder_user (id bigint primary key, legacy_id bigint unique, staff_id bigint, type varchar(50), status varchar(30), created_at datetime, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists equipment_archive (id bigint primary key, legacy_id bigint unique, model varchar(255) not null, name varchar(255) not null, parameter text null, amount int not null default 0, supplier_id bigint null, purchase_time datetime null, region varchar(255) null, responsible_user_id bigint null, document varchar(500) null, remark text null, status varchar(30) not null default 'normal', created_at datetime not null, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists equipment_item (id bigint primary key, legacy_id bigint unique, archive_id bigint not null, coding varchar(20) not null, equipment_code varchar(50) not null, work_status varchar(30) not null default 'normal', status varchar(30) not null default 'normal', created_at datetime not null, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists repair_order (id bigint primary key auto_increment, legacy_id bigint unique, repair_code varchar(30) not null, archive_id bigint not null, equipment_id bigint not null, register_user_id bigint null, register_time datetime null, content text null, register_image varchar(500) null, repair_user_id bigint null, assign_time datetime null, repair_time datetime null, repair_content text null, repair_image varchar(500) null, failure_cause_id bigint null, consuming_seconds int not null default 0, status varchar(30) not null, created_at datetime not null, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists plan_task (id bigint primary key, legacy_id bigint unique, coding varchar(20) not null, plan_id bigint not null, equipment_id bigint not null, task_user_id bigint null, type varchar(30) not null, status varchar(30) not null default 'pending', start_time datetime null, due_time datetime null, created_at datetime not null, updated_at datetime null, deleted_at datetime null)");
        jdbc.execute("create table if not exists equipment_record (id bigint primary key auto_increment, legacy_id bigint unique, equipment_id bigint not null, relate_id bigint null, add_user_id bigint null, name varchar(128) not null, type varchar(64) not null, content text null, status varchar(30) not null default 'normal', created_at datetime not null, updated_at datetime null, deleted_at datetime null)");
    }

    private boolean tableExists(String table) {
        Integer value = jdbc.queryForObject("select count(*) from information_schema.tables where table_schema=database() and table_name=?", Integer.class, table);
        return value != null && value > 0;
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private List<Map<String, Object>> queryCounts(String sql) {
        return jdbc.queryForList(sql);
    }
}
