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
