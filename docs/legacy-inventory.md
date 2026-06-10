# 旧系统只读盘点

## 服务器与项目

- 服务器：`120.55.190.26`
- Nginx：宝塔路径 `/www/server/nginx`
- 站点目录：`/www/wwwroot/wos.ezelearning.net/device_inspection`
- 框架：FastAdmin + ThinkPHP，PHP `>=7.2`
- 主要业务模块：`application/admin/controller/equipment` 与 `application/api/controller/equipment`

## 数据库

- 数据库类型：MySQL
- 库名：`device`
- 表前缀：`fa_`
- 业务表：
  - `fa_equipment_archive` 设备档案，29 条
  - `fa_equipment_equipment` 设备台账，151 条
  - `fa_equipment_plan` 巡检/保养计划，13 条
  - `fa_equipment_plan_task` 周期任务，653 条
  - `fa_equipment_record` 设备记录，29 条
  - `fa_equipment_repair` 维修工单，15 条
  - `fa_equipment_staff` 员工扩展，11 条
  - `fa_equipment_department` 部门，4 条
  - `fa_equipment_supplier` 供应商，20 条
  - `fa_equipment_failure_cause` 故障原因，5 条
- 权限与用户：
  - `fa_admin` 后台管理员，3 条
  - `fa_user` 前台/API 用户，12 条
  - `fa_auth_rule` 后台权限节点，166 条
  - `fa_auth_group`、`fa_auth_group_access` 后台角色关系

## 核心业务流

- 设备状态：`normal` 正常、`sickness` 带病、`repairing` 维修中、`scrapped` 报废。
- 维修单状态：`pending` 待接单、`registered` 已接单/维修中、`repaired` 已修复、`scrapped` 已报废。
- 巡检/保养任务状态：`pending` 待处理、`finish` 已完成、`overdue` 已逾期。
- 用户提交报修时，新增维修单并将设备状态改为 `repairing`。
- 管理人员接单或后台指派后，维修单进入 `registered` 并记录维修人员与派单时间。
- 维修登记完成后，维修单进入 `repaired` 或 `scrapped`，设备状态同步恢复 `normal` 或改为 `scrapped`，并追加设备记录。
- 巡检/保养计划按周期生成任务，任务完成后写入 `fa_equipment_record`。

## 重写影响

原计划中的“工单系统”需要按实际业务收敛为“设备巡检/维修工单系统”。新系统第一版应优先覆盖设备档案、设备台账、维修工单、巡检/保养任务、设备记录、用户/部门/权限，而不是泛化客服工单。
