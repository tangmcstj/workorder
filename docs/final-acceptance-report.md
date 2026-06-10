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
- 旧导入/导出权限节点已专项核对：`equipment/archive/import` 已实现为设备档案 CSV 导入，`equipment/archive/exportTag` 已实现为设备标签 CSV 导出。
- 已验证设备档案导出、设备标签导出、设备档案按 `id` 导入更新；导入测试后档案数量仍保持新旧一致。

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
