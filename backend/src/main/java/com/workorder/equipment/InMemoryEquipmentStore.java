package com.workorder.equipment;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryEquipmentStore {
    private final AtomicLong repairIds = new AtomicLong(100);
    private final AtomicLong recordIds = new AtomicLong(100);
    private final List<Archive> archives = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Repair> repairs = new ArrayList<>();
    private final List<PlanTask> tasks = new ArrayList<>();
    private final List<Record> records = new ArrayList<>();

    public InMemoryEquipmentStore() {
        archives.add(new Archive(1L, "KQ-600", "空压机", 8, "一号车间", "杭州设备供应商", "王主管"));
        archives.add(new Archive(2L, "DX-220", "低压配电柜", 12, "配电室", "上海电气", "李工"));
        archives.add(new Archive(3L, "WT-90", "循环水泵", 9, "动力站", "宁波泵业", "赵工"));

        items.add(new Item(1L, 1L, "EABCDEFG", "E260609-001", Enums.EquipmentWorkStatus.normal));
        items.add(new Item(2L, 1L, "EBCDEFGH", "E260609-002", Enums.EquipmentWorkStatus.repairing));
        items.add(new Item(3L, 2L, "ECDEFGHI", "E260609-003", Enums.EquipmentWorkStatus.normal));
        items.add(new Item(4L, 3L, "EDEFGHIJ", "E260609-004", Enums.EquipmentWorkStatus.scrapped));

        repairs.add(new Repair(1L, "R260609-001", 1L, 2L, "设备运行异响，压力波动明显", "张三",
                LocalDateTime.now().minusHours(6), "李工", LocalDateTime.now().minusHours(5), null, Enums.RepairStatus.registered));
        repairs.add(new Repair(2L, "R260609-002", 2L, 3L, "柜体温度偏高，需要检查", "王五",
                LocalDateTime.now().minusHours(2), null, null, null, Enums.RepairStatus.pending));
        repairs.add(new Repair(3L, "R260608-001", 3L, 4L, "水泵轴承损坏，已报废", "赵六",
                LocalDateTime.now().minusDays(1), "李工", LocalDateTime.now().minusDays(1).plusHours(1),
                LocalDateTime.now().minusHours(20), Enums.RepairStatus.scrapped));

        tasks.add(new PlanTask(1L, "TAAAAAAA", "inspection", 1L, "张三", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5), Enums.PlanTaskStatus.pending));
        tasks.add(new PlanTask(2L, "TBBBBBBB", "maintenance", 3L, "李四", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2), Enums.PlanTaskStatus.pending));
        tasks.add(new PlanTask(3L, "TCCCCCCC", "inspection", 2L, "李工", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), Enums.PlanTaskStatus.finish));

        records.add(new Record(1L, 2L, 1L, "维修结果：维修中", "repair", "李工", "已接单，等待备件", LocalDateTime.now().minusHours(5)));
        records.add(new Record(2L, 1L, 3L, "巡检结果：正常", "inspection", "李工", "{\"work_status\":\"正常\"}", LocalDateTime.now().minusDays(1)));
    }

    public List<Archive> archives() {
        return archives;
    }

    public List<Item> items() {
        return items;
    }

    public List<Repair> repairs() {
        return repairs;
    }

    public List<PlanTask> tasks() {
        return tasks;
    }

    public List<Record> records() {
        return records;
    }

    public Archive archive(Long id) {
        return archives.stream().filter(item -> Objects.equals(item.id, id)).findFirst().orElseThrow(() -> new IllegalArgumentException("设备档案不存在"));
    }

    public Item item(Long id) {
        return items.stream().filter(item -> Objects.equals(item.id, id)).findFirst().orElseThrow(() -> new IllegalArgumentException("设备不存在"));
    }

    public Repair repair(Long id) {
        return repairs.stream().filter(item -> Objects.equals(item.id, id)).findFirst().orElseThrow(() -> new IllegalArgumentException("维修工单不存在"));
    }

    public PlanTask task(Long id) {
        return tasks.stream().filter(item -> Objects.equals(item.id, id)).findFirst().orElseThrow(() -> new IllegalArgumentException("计划任务不存在"));
    }

    public Repair addRepair(Long equipmentId, String content) {
        Item item = item(equipmentId);
        Archive archive = archive(item.archiveId);
        item.workStatus = Enums.EquipmentWorkStatus.repairing;
        long id = repairIds.incrementAndGet();
        Repair repair = new Repair(id, "R" + java.time.format.DateTimeFormatter.ofPattern("yyMMdd").format(LocalDateTime.now()) + "-" + String.format("%03d", id),
                archive.id, item.id, content, "当前用户", LocalDateTime.now(), null, null, null, Enums.RepairStatus.pending);
        repairs.add(repair);
        return repair;
    }

    public void receiveRepair(Long id) {
        Repair repair = repair(id);
        if (repair.status != Enums.RepairStatus.pending) {
            throw new IllegalArgumentException("只有待接单工单允许接收");
        }
        repair.repairUser = "当前用户";
        repair.assignTime = LocalDateTime.now();
        repair.status = Enums.RepairStatus.registered;
    }

    public void finishRepair(Long id, Enums.RepairStatus status, String content) {
        if (status != Enums.RepairStatus.repaired && status != Enums.RepairStatus.scrapped) {
            throw new IllegalArgumentException("维修结果只能是已修复或已报废");
        }
        Repair repair = repair(id);
        if (repair.status != Enums.RepairStatus.registered) {
            throw new IllegalArgumentException("只有维修中工单允许登记完成");
        }
        repair.repairTime = LocalDateTime.now();
        repair.status = status;
        item(repair.equipmentId).workStatus = status == Enums.RepairStatus.scrapped ? Enums.EquipmentWorkStatus.scrapped : Enums.EquipmentWorkStatus.normal;
        records.add(new Record(recordIds.incrementAndGet(), repair.equipmentId, repair.id, "维修结果：" + status.text(), "repair", "当前用户", content, LocalDateTime.now()));
    }

    public void submitTask(Long id, Map<String, Object> content) {
        PlanTask task = task(id);
        if (task.status != Enums.PlanTaskStatus.pending) {
            throw new IllegalArgumentException("只有待处理任务允许提交");
        }
        task.status = Enums.PlanTaskStatus.finish;
        records.add(new Record(recordIds.incrementAndGet(), task.equipmentId, task.id,
                "inspection".equals(task.type) ? "巡检结果：" + content.getOrDefault("work_status", "已完成") : "保养完成",
                task.type, "当前用户", String.valueOf(content), LocalDateTime.now()));
    }

    public Map<Long, Archive> archiveMap() {
        Map<Long, Archive> map = new HashMap<>();
        archives.forEach(item -> map.put(item.id, item));
        return map;
    }

    public List<Repair> sortedRepairs() {
        return repairs.stream().sorted(Comparator.comparing((Repair repair) -> repair.registerTime).reversed()).toList();
    }

    public static class Archive {
        public Long id;
        public String model;
        public String name;
        public int amount;
        public String region;
        public String supplier;
        public String responsibleName;

        public Archive(Long id, String model, String name, int amount, String region, String supplier, String responsibleName) {
            this.id = id;
            this.model = model;
            this.name = name;
            this.amount = amount;
            this.region = region;
            this.supplier = supplier;
            this.responsibleName = responsibleName;
        }
    }

    public static class Item {
        public Long id;
        public Long archiveId;
        public String coding;
        public String equipmentCode;
        public Enums.EquipmentWorkStatus workStatus;

        public Item(Long id, Long archiveId, String coding, String equipmentCode, Enums.EquipmentWorkStatus workStatus) {
            this.id = id;
            this.archiveId = archiveId;
            this.coding = coding;
            this.equipmentCode = equipmentCode;
            this.workStatus = workStatus;
        }
    }

    public static class Repair {
        public Long id;
        public String repairCode;
        public Long archiveId;
        public Long equipmentId;
        public String content;
        public String registerUser;
        public LocalDateTime registerTime;
        public String repairUser;
        public LocalDateTime assignTime;
        public LocalDateTime repairTime;
        public Enums.RepairStatus status;

        public Repair(Long id, String repairCode, Long archiveId, Long equipmentId, String content, String registerUser, LocalDateTime registerTime, String repairUser, LocalDateTime assignTime, LocalDateTime repairTime, Enums.RepairStatus status) {
            this.id = id;
            this.repairCode = repairCode;
            this.archiveId = archiveId;
            this.equipmentId = equipmentId;
            this.content = content;
            this.registerUser = registerUser;
            this.registerTime = registerTime;
            this.repairUser = repairUser;
            this.assignTime = assignTime;
            this.repairTime = repairTime;
            this.status = status;
        }
    }

    public static class PlanTask {
        public Long id;
        public String coding;
        public String type;
        public Long equipmentId;
        public String taskUser;
        public LocalDateTime startTime;
        public LocalDateTime dueTime;
        public Enums.PlanTaskStatus status;

        public PlanTask(Long id, String coding, String type, Long equipmentId, String taskUser, LocalDateTime startTime, LocalDateTime dueTime, Enums.PlanTaskStatus status) {
            this.id = id;
            this.coding = coding;
            this.type = type;
            this.equipmentId = equipmentId;
            this.taskUser = taskUser;
            this.startTime = startTime;
            this.dueTime = dueTime;
            this.status = status;
        }
    }

    public record Record(Long id, Long equipmentId, Long relateId, String name, String type, String user, String content, LocalDateTime createdAt) {
    }
}
