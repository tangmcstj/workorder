# 最终验收记录

## 时间与环境

- 验收时间：2026-06-10
- 测试服务器：47.96.254.254
- 源码目录：`/opt/workorder-src`
- 构建目录：`/opt/workorder-build`
- 产物目录：`/opt/workorder-dist`
- 运行目录：`/www/wwwroot/workorder-backend`、`/www/wwwroot/workorder-frontend`

## 构建与部署

- 已使用 `/opt/workorder-build/build.sh /opt/workorder-src /opt/workorder-dist` 在容器中完成构建。
- 已清理 `/opt/workorder-dist` 后重新构建，前端产物目录无旧资源残留。
- 后端产物已部署到 `/www/wwwroot/workorder-backend/workorder-backend.jar`。
- 前端产物已部署到 `/www/wwwroot/workorder-frontend`。
- `workorder.service` 已重启并保持 `active`。
- `/actuator/health` 返回 `UP`。
- 后端 dist 与运行目录 JAR 哈希一致：`0e70de2cef40b3cd774cf8eac51ca719288a1acf24e406a4bf66f6f38124a37d`。

## 数据一致性

| 数据项 | 旧表 | 新表 |
| --- | ---: | ---: |
| 设备档案 | 29 | 29 |
| 设备台账 | 151 | 151 |
| 维修工单 | 15 | 15 |
| 计划任务 | 653 | 653 |
| 设备记录 | 35 | 35 |

状态核对结果：

- 设备有效状态：`normal=134`、`repairing=3`、`scrapped=2`，新旧一致。
- 维修有效状态：`registered=3`、`repaired=10`、`scrapped=2`，新旧一致。
- 计划任务总量：`pending=628`、`finish=18`、`overdue=7`，新旧一致。
- 计划任务有效数据：`pending=464`、`finish=18`、`overdue=7`，新旧一致。

## 接口与流程

已验证后台接口：

- 登录、工作台、设备档案、设备列表、维修列表、巡检/保养任务、系统配置、用户角色。
- 旧导入/导出权限节点已专项核对：`equipment/archive/import` 已实现为“下载模板 -> 上传 CSV -> 查看导入结果”的设备档案导入流程，`equipment/archive/exportTag` 已实现为设备标签 CSV 导出。
- 已验证设备档案导出、设备标签导出、设备档案按 `id` 导入更新；导入测试后档案数量仍保持新旧一致。
- 旧后台 FastAdmin 标准动作已补齐兼容 API：部门、员工、供应商、故障原因、提醒人员、设备档案支持新增、编辑、软删除、回收站、恢复、销毁、批量更新；员工支持微信解绑和选择器；巡检/保养计划支持列表、已停用、停用、设备明细、检查项、无效任务 dry-run 清理；设备记录支持列表和详情；维修工单支持详情、指派和后台登记。
- 新增“基础资料”页面 `/legacy-manage`，用于承载上述基础资料、计划和记录管理入口，保持现有蓝白后台 UI 风格。
- 本轮按旧 FastAdmin 页面 JS 和视图继续补齐页面级复刻：设备档案页恢复设备参数、采购时间、文档、备注、设备明细、维修/巡检/保养记录入口和回收站；设备列表页恢复二维码标签、运行状态、维修/巡检/保养记录入口和回收站；维修工单页恢复故障原因、提醒人员、指派、登记、详情、回收站；巡检/保养菜单恢复为旧后台“计划列表”口径，支持已停用、检查项、设备明细、记录明细、清理无效任务；侧边菜单补齐设备记录、员工管理、部门管理、供应商管理、故障原因、提醒人员独立入口。
- 用户角色、系统配置、菜单权限已专项复核：用户角色页补齐旧管理员、前台用户、员工、部门、角色权限、后台菜单、设备权限节点；系统配置页保留小程序配置、二维码域名、管理电话、自动派单、兼容接口状态，并新增菜单权限核对页签。当前接口核对结果为管理员 3、前台用户 12、角色 5、权限节点 166、旧后台菜单 24、设备权限节点 83。

已验证微信小程序兼容接口：

- `weapplogin`
- `workbench`
- `list`
- `info`
- `qrcode`
- `repairInfos`
- `planTaskFields`
- `getFailureCause`
- `getRecordInfo`
- `getRelationshipList`

已验证写入闭环，并在测试后恢复数据：

- 报修 -> 接单 -> 维修登记完成。
- 巡检/保养任务提交 -> 设备记录生成。
- 清理后 `repair_order=15`、`equipment_record=35`，设备状态和任务状态已恢复。

## 运行状态

- Nginx 前端 `/` 和 `/users` 均返回 `200 OK`。
- `/opt/workorder-dist/frontend` 与 `/www/wwwroot/workorder-frontend` 当前文件一致。
- 最近日志未发现 `ERROR`、`Exception`、`SQLSyntax`、`BadSql`。

## 备注

- 微信真实 `code -> openid` 仍依赖正式小程序 AppID/Secret；测试环境已用已绑定 `openid` 验证兼容登录。
- 旧生产库仍未写入，本轮写入闭环仅在测试库执行，并已清理恢复。
