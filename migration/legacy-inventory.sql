-- 在旧库 device 上只读执行，用于盘点表、字段、数据量。

select table_name, table_rows
from information_schema.tables
where table_schema = database()
order by table_name;

select table_name, column_name, column_type, is_nullable, column_default, column_comment
from information_schema.columns
where table_schema = database()
order by table_name, ordinal_position;

select status, count(*) from fa_equipment_repair group by status;
select work_status, count(*) from fa_equipment_equipment group by work_status;
select status, count(*) from fa_equipment_plan_task group by status;
