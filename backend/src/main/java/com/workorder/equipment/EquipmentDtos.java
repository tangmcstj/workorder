package com.workorder.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class EquipmentDtos {
    public record ArchiveDto(Long id, String model, String name, int amount, String region, String supplier, String responsibleName) {
    }

    public record EquipmentItemDto(
            Long id,
            Long archiveId,
            String coding,
            String equipmentCode,
            String archiveModel,
            String archiveName,
            String region,
            Enums.EquipmentWorkStatus workStatus,
            String workStatusText
    ) {
    }

    public record EquipmentDetailDto(EquipmentItemDto equipment, List<RecordDto> records, Map<String, Object> todos) {
    }

    public record RepairDto(
            Long id,
            String repairCode,
            Long equipmentId,
            String equipmentCode,
            String archiveModel,
            String archiveName,
            String content,
            String registerUser,
            LocalDateTime registerTime,
            String repairUser,
            LocalDateTime assignTime,
            LocalDateTime repairTime,
            Enums.RepairStatus status,
            String statusText
    ) {
    }

    public record CreateRepairRequest(@NotNull Long equipmentId, @NotBlank String content, String registerImage) {
    }

    public record FinishRepairRequest(@NotBlank String repairContent, String repairImage, @NotBlank String repairStatus, Long failureCauseId) {
    }

    public record PlanTaskDto(
            Long id,
            String coding,
            String type,
            Long equipmentId,
            String equipmentCode,
            String archiveName,
            String taskUser,
            LocalDateTime startTime,
            LocalDateTime dueTime,
            Enums.PlanTaskStatus status,
            String statusText
    ) {
    }

    public record SubmitPlanTaskRequest(Map<String, Object> content) {
    }

    public record RecordDto(Long id, Long equipmentId, Long relateId, String name, String type, String user, String content, LocalDateTime createdAt) {
    }

    public record DashboardSummary(
            long archiveCount,
            long equipmentCount,
            long equipmentNormalCount,
            long equipmentRepairingCount,
            long equipmentScrappedCount,
            long repairPendingCount,
            long repairRegisteredCount,
            long inspectionPendingCount,
            long maintenancePendingCount
    ) {
    }
}
