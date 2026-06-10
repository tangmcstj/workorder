package com.workorder.config;

public class SystemConfigDtos {
    public record SystemConfig(
            String managePhone,
            String weappId,
            String weappSecretMasked,
            String qrcodeDomain,
            boolean repairAssignOneself,
            long reminderUserCount,
            long staffBoundOpenidCount
    ) {
    }

    public record UpdateSystemConfig(
            String managePhone,
            String weappId,
            String weappSecret,
            String qrcodeDomain,
            boolean repairAssignOneself
    ) {
    }
}
