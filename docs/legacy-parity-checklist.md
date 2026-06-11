# 新旧系统一致性核对清单

## 后台菜单

| 旧系统模块 | 新系统页面/API | 当前状态 | 核对要点 |
| --- | --- | --- | --- |
| 工作台 | `/dashboard`, `/api/dashboard/summary` | 已接入新表 | 档案、设备、维修、巡检、保养数量与旧库一致 |
| 设备档案 | `/archives`, `/api/equipment/archives` | 已接入新表 | 型号、名称、供应商、负责人、区域、文档路径 |
| 设备列表 | `/equipment`, `/api/equipment/items` | 已接入新表 | 二维码编码、设备编号、运行状态、详情时间线 |
| 维修工单 | `/repairs`, `/api/repairs` | 已接入新表 | 待接单、维修中、已修复、已报废状态含义 |
| 巡检任务 | `/inspection`, `/api/plan-tasks?type=inspection` | 已接入新表 | 任务字段、执行人、完成记录 |
| 保养任务 | `/maintenance`, `/api/plan-tasks?type=maintenance` | 已接入新表 | 任务字段、执行人、完成记录 |
| 用户角色 | `/users`, `/api/admin/users-overview` | 已替换占位页 | 用户、员工、部门、旧角色只读核对 |
| 系统配置 | `/settings`, `/api/config/system` | 已完善 | 小程序配置、二维码域名、提醒人、微信绑定统计 |

## 旧后台权限节点复刻

| 旧节点类别 | 新系统接口 | 当前状态 | 核对要点 |
| --- | --- | --- | --- |
| 部门管理 | `/api/admin/legacy/departments` | 已补齐 CRUD/回收站/批量 | `equipment_manage`、状态、软删除 |
| 员工管理 | `/api/admin/legacy/staff`、`/api/admin/legacy/staff-picker` | 已补齐 CRUD/选择器/解绑 | 用户关联、部门、工号、openid |
| 供应商管理 | `/api/admin/legacy/suppliers` | 已补齐 CRUD/回收站/批量 | 联系人、银行、合作关系、软删除 |
| 故障原因 | `/api/admin/legacy/failure-causes` | 已补齐 CRUD/回收站/批量 | 小程序和维修登记共用 |
| 提醒人员 | `/api/admin/legacy/reminder-users` | 已补齐 CRUD/回收站/批量 | 人员、提醒类型、状态 |
| 设备档案标准动作 | `/api/admin/legacy/archives` | 已补齐 CRUD/回收站/批量 | 与档案导入导出共用新表 |
| 设备档案导入 | `/api/admin/import/archives-template.csv`、`/api/admin/import/archives` | 已补齐模板下载、上传、结果统计 | 新增、按 id 更新、错误行返回 |
| 设备档案导出 | `/api/admin/export/archives.csv`、`/api/admin/export/archive-tags.csv` | 已补齐导出和标签导出 | CSV 字段、二维码编码 |
| 巡检/保养计划 | `/api/admin/legacy/plans` | 已补齐列表、停用、已停用、设备明细 | 类型、状态、任务数量 |
| 计划检查项 | `/api/admin/legacy/plans/{id}/fields` | 已补齐 | 字段排序、必填、选项 |
| 清理无效任务 | `/api/admin/legacy/plans/clear-invalid-task` | 已补齐，默认 dry-run | 停用计划、报废设备、缺失关联 |
| 设备记录 | `/api/admin/legacy/records` | 已补齐列表和详情 | 维修/巡检/保养记录内容 |
| 维修后台动作 | `/api/admin/legacy/repairs/{id}` | 已补齐详情、指派、登记 | 状态流转、设备状态、记录生成 |

## 微信小程序兼容接口

| 旧接口 | 新实现 | 返回格式 | 当前状态 |
| --- | --- | --- | --- |
| `/api/equipment/login` | 新表用户和员工绑定 | `{code,msg,data}` | 已实现 |
| `/api/equipment/weapplogin` | openid 绑定员工登录 | `{code,msg,data}` | 已实现 |
| `/api/equipment/logout` | 解除绑定 | `{code,msg,data}` | 已实现 |
| `/api/equipment/unbind` | 解除绑定别名 | `{code,msg,data}` | 已实现 |
| `/api/equipment/workbench` | 工作台统计 | `{code,msg,data}` | 已实现 |
| `/api/equipment/archives` | 档案列表 | `{code,msg,data}` | 已实现 |
| `/api/equipment/list` | 设备列表 | `{code,msg,data}` | 已实现 |
| `/api/equipment/info` | 设备详情 | `{code,msg,data}` | 已实现 |
| `/api/equipment/repairs` | GET 维修列表 / POST 报修 | `{code,msg,data}` | 已实现 |
| `/api/equipment/receiveRepairs` | 接单 | `{code,msg,data}` | 已实现 |
| `/api/equipment/repairInfos` | 维修详情 | `{code,msg,data}` | 已实现 |
| `/api/equipment/registers` | 完成维修登记 | `{code,msg,data}` | 已实现 |
| `/api/equipment/getFailureCause` | 故障原因 | `{code,msg,data}` | 已实现 |
| `/api/equipment/planTaskFields` | 动态任务字段 | `{code,msg,data}` | 已实现 |
| `/api/equipment/submitPlanTasks` | 提交巡检/保养 | `{code,msg,data}` | 已实现 |
| `/api/equipment/getRecordInfo` | 记录详情 | `{code,msg,data}` | 已实现 |
| `/api/equipment/qrcode` | 二维码路径 | `{code,msg,data}` | 已实现 |
| `/api/equipment/getRelationshipList` | 员工绑定关系 | `{code,msg,data}` | 已实现 |

## 数据核对基准

测试库导入旧表后，迁移报告应至少覆盖这些基准数量：

| 数据 | 旧表数量 |
| --- | ---: |
| 设备档案 `fa_equipment_archive` | 29 |
| 设备台账 `fa_equipment_equipment` | 151 |
| 计划任务 `fa_equipment_plan_task` | 653 |
| 维修工单 `fa_equipment_repair` | 15 |
| 员工 `fa_equipment_staff` | 11 |
| 前台用户 `fa_user` | 12 |
| 权限节点 `fa_auth_rule` | 166 |

状态分布以 `/api/migration/legacy/report` 和 `migration/parity-check.sql` 输出为准。计划任务同时核对总量口径和有效数据口径：当前总量 pending 为 628，有效数据 pending 为 464。发现数量或状态差异时，优先修迁移映射，再重新执行迁移和核对。
