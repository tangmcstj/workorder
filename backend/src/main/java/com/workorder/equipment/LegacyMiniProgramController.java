package com.workorder.equipment;

import com.workorder.common.LegacyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
public class LegacyMiniProgramController {
    private final LegacyMiniProgramService service;

    public LegacyMiniProgramController(LegacyMiniProgramService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LegacyResponse<Map<String, Object>> login(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String openid
    ) {
        return LegacyResponse.success("Logged in successful", service.login(mobile, password, code, openid));
    }

    @PostMapping("/weapplogin")
    public LegacyResponse<Map<String, Object>> weappLogin(@RequestParam(required = false) String code, @RequestParam(required = false) String openid) {
        return LegacyResponse.success("Logged in successful", service.weappLogin(code, openid));
    }

    @PostMapping("/logout")
    public LegacyResponse<Void> logout(@RequestParam(required = false) Long user_id, @RequestParam(required = false) String openid) {
        service.logout(user_id, openid);
        return LegacyResponse.success("Logout successful", null);
    }

    @PostMapping("/unbind")
    public LegacyResponse<Void> unbind(@RequestParam(required = false) Long user_id, @RequestParam(required = false) String openid) {
        service.unbind(user_id, openid);
        return LegacyResponse.success("Operation completed", null);
    }

    @GetMapping("/getSystemInfo")
    public LegacyResponse<Map<String, Object>> systemInfo() {
        return LegacyResponse.success("", service.systemInfo());
    }

    @GetMapping("/getStaffInfo")
    public LegacyResponse<Map<String, Object>> staffInfo(@RequestParam(required = false) Long user_id, @RequestParam(required = false) String openid) {
        return LegacyResponse.success("", service.staffInfo(user_id, openid));
    }

    @GetMapping("/workbench")
    public LegacyResponse<Map<String, Object>> workbench(@RequestParam(required = false) Long user_id, @RequestParam(required = false) String openid) {
        return LegacyResponse.success("", service.workbench(user_id, openid));
    }

    @RequestMapping({"/list", "/lists", "/manage/equipments"})
    public LegacyResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String archive_keyword,
            @RequestParam(required = false) String equipment_keyword,
            @RequestParam(required = false) String plan_type,
            @RequestParam(required = false) Long archive_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return LegacyResponse.success("", service.equipments(status, archive_keyword, equipment_keyword, plan_type, archive_id, page, pageSize));
    }

    @RequestMapping({"/info", "/detail", "/equipments"})
    public LegacyResponse<Map<String, Object>> info(@RequestParam(required = false) String coding, @RequestParam(required = false) String type) {
        return LegacyResponse.success("", service.equipmentInfo(coding, type));
    }

    @PostMapping("/repairs")
    public LegacyResponse<Void> createRepair(
            @RequestParam(required = false) Long equipment_id,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String register_image,
            @RequestParam(required = false) Long user_id,
            @RequestParam(required = false) String openid
    ) {
        service.createRepair(equipment_id, content, register_image, user_id, openid);
        return LegacyResponse.success("Operation completed", null);
    }

    @GetMapping("/repairs")
    public LegacyResponse<Map<String, Object>> repairs(
            @RequestParam(required = false) Long user_id,
            @RequestParam(required = false) String openid,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return LegacyResponse.success("", service.repairs(user_id, openid, type, page, pageSize));
    }

    @PostMapping("/receiveRepairs")
    public LegacyResponse<Void> receiveRepair(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long user_id,
            @RequestParam(required = false) String openid
    ) {
        service.receiveRepair(id, user_id, openid);
        return LegacyResponse.success("Operation completed", null);
    }

    @RequestMapping("/repairInfos")
    public LegacyResponse<Map<String, Object>> repairInfo(@RequestParam(required = false) Long id) {
        return LegacyResponse.success("", service.repairInfo(id));
    }

    @PostMapping("/registers")
    public LegacyResponse<Void> registerRepair(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String repair_status,
            @RequestParam(required = false) String repair_content,
            @RequestParam(required = false) String repair_image,
            @RequestParam(required = false) Long failure_cause_id,
            @RequestParam(required = false) Long user_id,
            @RequestParam(required = false) String openid,
            @RequestParam Map<String, Object> params
    ) {
        String targetStatus = repair_status != null ? repair_status : stringParam(params, "status");
        String content = repair_content != null ? repair_content : stringParam(params, "content");
        String image = repair_image != null ? repair_image : stringParam(params, "image");
        service.registerRepair(id, targetStatus, content, image, failure_cause_id, user_id, openid);
        return LegacyResponse.success("Operation completed", null);
    }

    @GetMapping("/getFailureCause")
    public LegacyResponse<Object> failureCauses() {
        return LegacyResponse.success("", service.failureCauses());
    }

    @RequestMapping("/planTaskFields")
    public LegacyResponse<Object> planTaskFields(@RequestParam(required = false) Long id) {
        return LegacyResponse.success("", service.planTaskFields(id));
    }

    @PostMapping("/submitPlanTasks")
    public LegacyResponse<Void> submitPlanTask(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String type,
            @RequestParam Map<String, Object> params
    ) {
        Long userId = params.get("user_id") == null ? null : Long.valueOf(String.valueOf(params.get("user_id")));
        String openid = params.get("openid") == null ? null : String.valueOf(params.get("openid"));
        service.submitPlanTask(id, type, params, userId, openid);
        return LegacyResponse.success("Operation completed", null);
    }

    @RequestMapping("/getRecordInfo")
    public LegacyResponse<Map<String, Object>> recordInfo(@RequestParam(required = false) Long id) {
        return LegacyResponse.success("", service.recordInfo(id));
    }

    @RequestMapping({"/qrcode", "/qrCode"})
    public LegacyResponse<Map<String, Object>> qrcode(@RequestParam(required = false) String coding) {
        return LegacyResponse.success("", service.qrcode(coding));
    }

    @RequestMapping("/getRelationshipList")
    public LegacyResponse<Object> relationshipList() {
        return LegacyResponse.success("", service.relationshipList());
    }

    private String stringParam(Map<String, Object> params, String name) {
        Object value = params.get(name);
        return value == null ? null : String.valueOf(value);
    }
}
