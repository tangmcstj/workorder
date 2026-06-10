# 工单系统重写版

基于 Spring Boot 3 + Vue 3 重写工单系统。当前版本是可运行的迁移前骨架：先提供核心领域模型、接口契约、前端工作台、部署模板和迁移说明；等旧服务器代码与数据库信息补齐后，再替换为真实 MySQL/MyBatis-Plus 持久化与数据迁移映射。

服务器盘点后确认旧系统实际业务为设备巡检/保养/维修工单系统，新 UI 视觉沿用：蓝色皮肤、左侧菜单、顶部白色导航、白底 panel、toolbar、Bootstrap 风格表格与状态标签。

## 目录

- `backend/` Spring Boot 后端
- `frontend/` Vue 3 + TypeScript + Element Plus 前端
- `deploy/` Nginx 与 systemd 部署模板
- `migration/` 旧系统盘点与迁移脚本占位
- `docs/` 架构、接口、迁移说明

## 本地启动

后端需要 JDK 21 与 Maven：

```bash
cd backend
mvn spring-boot:run
```

前端需要 Node.js 与 npm：

```bash
cd frontend
npm install
npm run dev
```

默认账号：

- 管理员：`admin` / `admin123`
- 坐席：`agent` / `agent123`

## 下一步

1. 提供旧服务器 SSH、PHP 项目路径、数据库只读账号。
2. 根据 `migration/legacy-inventory.sql` 与 `docs/migration.md` 做只读盘点。
3. 将当前内存仓库替换为 MySQL + MyBatis-Plus 实现。
4. 编写旧库到新库的可重复迁移任务。
