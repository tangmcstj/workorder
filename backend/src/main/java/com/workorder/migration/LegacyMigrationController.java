package com.workorder.migration;

import com.workorder.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/migration")
public class LegacyMigrationController {
    private final LegacyMigrationService service;

    public LegacyMigrationController(LegacyMigrationService service) {
        this.service = service;
    }

    @PostMapping("/legacy/run")
    public ApiResponse<Map<String, Object>> run() {
        return ApiResponse.ok(service.migrate());
    }

    @GetMapping("/legacy/report")
    public ApiResponse<Map<String, Object>> report() {
        service.ensureMigrated();
        return ApiResponse.ok(service.report());
    }
}
