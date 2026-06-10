# 迁移方案

## 原则

- 旧库只读，新库写入。
- 先迁移基础资料，再迁移业务流水。
- 新表保留 `legacy_id`，并在测试迁移中保持旧主键作为新表主键，方便小程序旧参数继续可用。
- FastAdmin 的 Unix 秒级时间统一转换为新库 `datetime`。

## 建议顺序

1. 用户与权限：`fa_admin`、`fa_user`、`fa_auth_group`、`fa_auth_group_access`、`fa_auth_rule`
2. 基础资料：部门、员工、供应商、故障原因
3. 设备资料：设备档案、设备台账
4. 计划资料：巡检/保养计划、计划字段、计划人员、计划设备范围
5. 业务流水：计划任务、维修工单、设备记录、附件

## 字段映射重点

- `fa_user` -> `app_user`
- `fa_equipment_department` -> `department`
- `fa_equipment_staff` -> `staff`
- `fa_equipment_supplier` -> `supplier`
- `fa_equipment_failure_cause` -> `failure_cause`
- `fa_equipment_archive` -> `equipment_archive`
- `fa_equipment_equipment` -> `equipment_item`
- `fa_equipment_plan` -> `work_plan`
- `fa_equipment_plan_field` -> `plan_field`
- `fa_equipment_plan_user` -> `plan_user`
- `fa_equipment_plan_archive` -> `plan_archive`
- `fa_equipment_repair` -> `repair_order`
- `fa_equipment_plan_task` -> `plan_task`
- `fa_equipment_record` -> `equipment_record`
- `fa_equipment_reminder_users` -> `reminder_user`
- `deletetime is not null` 的数据迁移为软删除状态，默认不在列表显示。
- 旧附件路径原样保存，后续下载时由文件服务兼容解析。

## 执行方式

- 服务启动和主要业务接口会调用 `LegacyMigrationService.ensureMigrated()`，当新表为空且旧 `fa_*` 表存在时自动迁移。
- 也可以手动调用 `POST /api/migration/legacy/run` 重新执行迁移；迁移使用 `legacy_id`/主键 upsert，可重复执行。
- 核对接口：`GET /api/migration/legacy/report`。
- SQL 核对脚本：`migration/parity-check.sql`。

## 验收

- 每张表输出旧表数量、新表数量、跳过数量、失败数量。
- 抽样核对 5 台设备的档案、台账、维修记录、巡检记录是否完整。
- 抽样核对所有维修状态和设备状态组合是否符合旧系统规则。
