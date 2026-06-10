package com.workorder.config;

import com.workorder.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class SystemConfigController {
    private final SystemConfigService service;

    public SystemConfigController(SystemConfigService service) {
        this.service = service;
    }

    @GetMapping("/system")
    public ApiResponse<SystemConfigDtos.SystemConfig> get() {
        return ApiResponse.ok(service.getConfig());
    }

    @PutMapping("/system")
    public ApiResponse<SystemConfigDtos.SystemConfig> update(@RequestBody SystemConfigDtos.UpdateSystemConfig request) {
        return ApiResponse.ok(service.update(request));
    }
}
