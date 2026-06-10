select 'archive' item, (select count(*) from fa_equipment_archive) legacy_count, (select count(*) from equipment_archive) new_count;
select 'equipment' item, (select count(*) from fa_equipment_equipment) legacy_count, (select count(*) from equipment_item) new_count;
select 'repair' item, (select count(*) from fa_equipment_repair) legacy_count, (select count(*) from repair_order) new_count;
select 'plan_task' item, (select count(*) from fa_equipment_plan_task) legacy_count, (select count(*) from plan_task) new_count;
select 'staff' item, (select count(*) from fa_equipment_staff) legacy_count, (select count(*) from staff) new_count;
select 'user' item, (select count(*) from fa_user) legacy_count, (select count(*) from app_user) new_count;
select 'record' item, (select count(*) from fa_equipment_record) legacy_count, (select count(*) from equipment_record) new_count;

select 'legacy_equipment_status_active' scope, work_status status, count(*) total
from fa_equipment_equipment
where deletetime is null
group by work_status
order by work_status;

select 'new_equipment_status_active' scope, work_status status, count(*) total
from equipment_item
where deleted_at is null
group by work_status
order by work_status;

select 'legacy_repair_status_active' scope, status, count(*) total
from fa_equipment_repair
where deletetime is null
group by status
order by status;

select 'new_repair_status_active' scope, status, count(*) total
from repair_order
where deleted_at is null
group by status
order by status;

select 'legacy_task_status_total' scope, status, count(*) total
from fa_equipment_plan_task
group by status
order by status;

select 'new_task_status_total' scope, status, count(*) total
from plan_task
group by status
order by status;

select 'legacy_task_status_active' scope, status, count(*) total
from fa_equipment_plan_task
where deletetime is null
group by status
order by status;

select 'new_task_status_active' scope, status, count(*) total
from plan_task
where deleted_at is null
group by status
order by status;

select e.id, e.coding, e.equipment_code,
       e.work_status legacy_status, n.work_status new_status,
       e.archive_id legacy_archive_id, n.archive_id new_archive_id
from fa_equipment_equipment e
left join equipment_item n on n.legacy_id=e.id
order by e.id asc
limit 5;
