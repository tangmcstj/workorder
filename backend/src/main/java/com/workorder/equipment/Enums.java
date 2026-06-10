package com.workorder.equipment;

public class Enums {
    public enum EquipmentWorkStatus {
        normal("正常运行"),
        sickness("带病运行"),
        repairing("停机待修"),
        scrapped("报废停用");

        private final String text;

        EquipmentWorkStatus(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    public enum RepairStatus {
        pending("待接单"),
        registered("维修中"),
        repaired("已修复"),
        scrapped("已报废");

        private final String text;

        RepairStatus(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    public enum PlanTaskStatus {
        pending("待处理"),
        finish("已完成"),
        overdue("已逾期");

        private final String text;

        PlanTaskStatus(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }
}
