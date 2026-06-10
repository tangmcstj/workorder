# API 约定

统一响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

分页响应：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 20
}
```

## 认证

- `POST /api/auth/login` 登录
- `GET /api/auth/me` 当前用户占位

## 工作台

- `GET /api/dashboard/summary` 工作台统计

## 设备

- `GET /api/equipment/archives` 设备档案
- `GET /api/equipment/items` 设备台账
- `GET /api/equipment/items/{id}` 设备详情、待办和记录

## 维修

- `GET /api/repairs` 维修工单列表，参数 `type=pending|registered|finish`
- `POST /api/repairs` 创建报修
- `POST /api/repairs/{id}/receive` 接收工单
- `POST /api/repairs/{id}/finish` 维修登记完成

## 巡检/保养

- `GET /api/plan-tasks` 计划任务列表，参数 `type=inspection|maintenance`
- `POST /api/plan-tasks/{id}/submit` 完成任务

## 用户与系统

- `GET /api/admin/users-overview` 用户、员工、部门、旧角色只读核对
- `POST /api/admin/import/archives` 设备档案 CSV 导入，兼容旧 `equipment/archive/import`
- `GET /api/admin/export/archives.csv` 设备档案导出
- `GET /api/admin/export/archive-tags.csv` 设备标签数据导出，兼容旧 `equipment/archive/exportTag`
- `GET /api/config/system` 系统配置
- `PUT /api/config/system` 保存系统配置，`weappSecret` 掩码值不会覆盖原密钥
- `GET /api/migration/legacy/report` 新旧表数量和状态分布报告
- `POST /api/migration/legacy/run` 手动执行一次旧表到新表迁移

## 微信小程序兼容接口

小程序兼容接口保留旧响应格式 `{code,msg,data}`，错误仍返回旧式 `code=0`。

- `POST /api/equipment/login`
- `POST /api/equipment/weapplogin`
- `POST /api/equipment/logout`
- `POST /api/equipment/unbind`
- `GET /api/equipment/getSystemInfo`
- `GET /api/equipment/getStaffInfo`
- `GET /api/equipment/workbench`
- `GET|POST /api/equipment/list`
- `GET|POST /api/equipment/info`
- `GET /api/equipment/repairs`
- `POST /api/equipment/repairs`
- `POST /api/equipment/receiveRepairs`
- `GET|POST /api/equipment/repairInfos`
- `POST /api/equipment/registers`
- `GET /api/equipment/getFailureCause`
- `GET|POST /api/equipment/planTaskFields`
- `POST /api/equipment/submitPlanTasks`
- `GET|POST /api/equipment/getRecordInfo`
- `GET|POST /api/equipment/qrcode`
- `GET|POST /api/equipment/getRelationshipList`

## 当前实现状态

后台业务服务和小程序兼容服务均已切到新规范表；旧 `fa_*` 表只作为迁移来源、兼容角色读取和核对基准。
