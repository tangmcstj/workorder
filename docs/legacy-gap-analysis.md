# Legacy Gap Analysis

## 2026-06-09 Findings

旧系统是 FastAdmin 插件式设备工单系统，核心代码在生产机：

- `/www/wwwroot/wos.ezelearning.net/device_inspection/addons/equipment`
- `/www/wwwroot/wos.ezelearning.net/device_inspection/application/admin/controller/equipment`
- `/www/wwwroot/wos.ezelearning.net/device_inspection/application/api/controller/equipment`
- `/www/wwwroot/wos.ezelearning.net/device_inspection/addons/equipment/uniapp`

新系统当前必须以旧系统真实表结构和状态值为准，不能继续使用内存样例数据。

## Legacy Functional Surface

后台菜单与功能包括：

- 看板中心
- 部门管理
- 员工管理，包含微信小程序解绑
- 供应商管理
- 设备档案
- 设备列表
- 巡检计划、巡检任务
- 保养计划、保养任务
- 维修管理、维修工单
- 故障原因
- 提醒人员
- FastAdmin 标准的新增、编辑、删除、批量、回收站、恢复、销毁、导入、导出、选择器等节点

旧系统 API/控制器方法包括：

- 登录与小程序：`login`、`weapplogin`、`logout`、`unbind`
- 工作台与查询：`workbench`、`list`、`lists`、`detail`、`info`
- 设备与档案：`archives`、`equipments`、`qrcode`、`exportTag`
- 维修：`register`、`registers`、`repairs`、`repairInfos`、`receiveRepairs`
- 计划任务：`planTaskFields`、`submitPlanTasks`、`clearInvalidTask`
- 基础资料：`getFailureCause`、`getStaffInfo`、`getSystemInfo`、`getRelationshipList`

## Legacy Data Counts

生产库旧表只读盘点结果：

| Table | Count |
| --- | ---: |
| `fa_equipment_archive` | 29 |
| `fa_equipment_department` | 4 |
| `fa_equipment_equipment` | 151 |
| `fa_equipment_failure_cause` | 5 |
| `fa_equipment_plan` | 13 |
| `fa_equipment_plan_archive` | 23 |
| `fa_equipment_plan_field` | 18 |
| `fa_equipment_plan_task` | 653 |
| `fa_equipment_plan_user` | 19 |
| `fa_equipment_record` | 29 |
| `fa_equipment_reminder_users` | 4 |
| `fa_equipment_repair` | 15 |
| `fa_equipment_staff` | 11 |
| `fa_equipment_supplier` | 20 |
| `fa_admin` | 3 |
| `fa_auth_group` | 5 |
| `fa_auth_rule` | 166 |
| `fa_user` | 12 |

关键枚举现状：

- 设备 `work_status`: `normal` 146, `repairing` 3, `scrapped` 2
- 维修 `status`: `registered` 3, `repaired` 10, `scrapped` 2
- 计划任务 `status`: `pending` 628, `finish` 18, `overdue` 7
- 计划 `type`: `inspection` 7, `maintenance` 6

## WeChat Mini Program Compatibility

旧系统微信小程序在 `addons/equipment/uniapp`，核心后端是 `application/api/controller/equipment/User.php`。

必须保留或兼容：

- `Staff.openid` 绑定关系
- `login()` 手机号/密码登录，并支持传入微信 `code` 后绑定 openid
- `weapplogin()` 通过 `code` 或 `openid` 自动登录员工
- 员工所属部门的 `equipment_manage` 权限
- 插件配置中的 `weappid`、`weappsecret`
- 小程序端使用的维修登记、维修处理、巡检/保养任务提交、设备二维码/详情、基础字典接口

## Current New-System Gaps

已修正：

- 测试库已授权导入旧业务和用户表，可作为一致性核对基准。
- 新增 `LegacyMigrationService`，将旧 `fa_*` 表迁移到新规范表，保留 `legacy_id` 并支持重复 upsert。
- 后台 `EquipmentService` 已改为读写新表：设备档案、设备台账、维修工单、巡检/保养任务、设备记录。
- 微信小程序兼容 API 已补齐并改为读写新表：登录、绑定/解绑、工作台、设备列表/详情、报修、接单、维修登记、任务字段、任务提交、记录详情、二维码和绑定关系。
- 系统配置页已替换占位页，支持小程序配置、管理电话、二维码域名、自动派单开关、提醒人员与微信绑定统计。
- 用户角色页已替换占位页，展示用户、员工、部门和旧角色权限数据。
- 新增 `docs/legacy-parity-checklist.md` 和 `migration/parity-check.sql`，用于后续逐项验收。
- 微信真实 `code -> openid` 已接入微信 `jscode2session`；测试环境仍支持直接传 `openid` 或 `code=openid:<openid>`。
- 新增附件上传/访问接口，文件落地到 `UPLOAD_ROOT`，返回路径可继续写入旧附件字段。
- 新增后台 CSV 导出接口，覆盖设备档案、设备台账、维修工单、计划任务；前端档案页已替换不可用的导入/编辑/删除占位按钮。

已收敛但不纳入第一版业务等价范围：

- FastAdmin 的回收站、恢复、销毁、批量导入、选择器等框架型后台能力不影响当前设备工单核心流程，第一版不做完全复刻。
- 按钮级权限已保留为菜单/接口权限模型和旧角色只读核对；如需做到 FastAdmin 节点级按钮控制，应作为权限专项继续拆分。
- 旧附件文件实体仍保留旧路径字段；新上传文件已统一进入 `UPLOAD_ROOT`，后续若要迁移历史实体文件到对象存储，需要单独执行文件盘点和路径替换计划。

## Build Status

测试机 `/opt/workorder-build/build.sh /opt/workorder-src /opt/workorder-dist` 已可完整构建：

- 后端产物：`/opt/workorder-dist/backend/workorder-backend.jar`
- 前端产物：`/opt/workorder-dist/frontend`

部署说明：

- 测试机继续使用 `/opt/workorder-build/build.sh /opt/workorder-src /opt/workorder-dist` 隔离构建。
- 构建后部署到 `/www/wwwroot/workorder-backend` 和 `/www/wwwroot/workorder-frontend`，重启 `workorder.service`。
- 部署后必须执行 `/api/migration/legacy/report` 和 `migration/parity-check.sql` 核对数量与状态分布。
