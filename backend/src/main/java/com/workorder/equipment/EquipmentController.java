package com.workorder.equipment;

import com.workorder.common.ApiResponse;
import com.workorder.common.LegacyResponse;
import com.workorder.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EquipmentController {
    private final EquipmentService service;
    private final LegacyMiniProgramService legacyService;

    public EquipmentController(EquipmentService service, LegacyMiniProgramService legacyService) {
        this.service = service;
        this.legacyService = legacyService;
    }

    @GetMapping("/api/dashboard/summary")
    public ApiResponse<EquipmentDtos.DashboardSummary> summary() {
        return ApiResponse.ok(service.summary());
    }

    @RequestMapping("/api/equipment/archives")
    public Object archives(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer pageSize
    ) {
        if (pageSize != null) {
            return LegacyResponse.success("", legacyService.archives(page, pageSize));
        }
        return ApiResponse.ok(service.archives(page, size));
    }

    @GetMapping("/api/equipment/items")
    public ApiResponse<PageResult<EquipmentDtos.EquipmentItemDto>> items(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(service.items(page, size, status));
    }

    @GetMapping("/api/equipment/items/{id}")
    public ApiResponse<EquipmentDtos.EquipmentDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @GetMapping("/api/repairs")
    public ApiResponse<PageResult<EquipmentDtos.RepairDto>> repairs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type
    ) {
        return ApiResponse.ok(service.repairs(page, size, type));
    }

    @PostMapping("/api/repairs")
    public ApiResponse<EquipmentDtos.RepairDto> createRepair(@Valid @RequestBody EquipmentDtos.CreateRepairRequest request) {
        return ApiResponse.ok(service.createRepair(request));
    }

    @PostMapping("/api/repairs/{id}/receive")
    public ApiResponse<Void> receiveRepair(@PathVariable Long id) {
        service.receiveRepair(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/repairs/{id}/finish")
    public ApiResponse<Void> finishRepair(@PathVariable Long id, @Valid @RequestBody EquipmentDtos.FinishRepairRequest request) {
        service.finishRepair(id, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/plan-tasks")
    public ApiResponse<PageResult<EquipmentDtos.PlanTaskDto>> planTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type
    ) {
        return ApiResponse.ok(service.planTasks(page, size, type));
    }

    @PostMapping("/api/plan-tasks/{id}/submit")
    public ApiResponse<Void> submitTask(@PathVariable Long id, @RequestBody EquipmentDtos.SubmitPlanTaskRequest request) {
        service.submitTask(id, request);
        return ApiResponse.ok(null);
    }
}
