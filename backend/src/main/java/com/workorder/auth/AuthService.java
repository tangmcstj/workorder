package com.workorder.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Map<String, UserAccount> users;

    public AuthService(PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.users = Map.of(
                "admin", new UserAccount(1L, "admin", "系统管理员", passwordEncoder.encode("admin123"),
                        Set.of("ADMIN"), Set.of("equipment:read", "equipment:write", "equipment:assign", "user:manage", "config:manage")),
                "agent", new UserAccount(2L, "agent", "工单坐席", passwordEncoder.encode("agent123"),
                        Set.of("AGENT"), Set.of("equipment:read", "equipment:write"))
        );
    }

    public AuthDtos.LoginResponse login(String username, String password) {
        UserAccount user = users.get(username);
        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return new AuthDtos.LoginResponse(jwtService.issue(user), profile(user), menus(user.permissions()));
    }

    private AuthDtos.UserProfile profile(UserAccount user) {
        return new AuthDtos.UserProfile(user.id(), user.username(), user.displayName(), user.roles(), user.permissions());
    }

    private List<AuthDtos.MenuItem> menus(Set<String> permissions) {
        return List.of(
                new AuthDtos.MenuItem("工作台", "/dashboard", "Monitor", "equipment:read"),
                new AuthDtos.MenuItem("设备档案", "/archives", "Folder", "equipment:read"),
                new AuthDtos.MenuItem("设备列表", "/equipment", "List", "equipment:read"),
                new AuthDtos.MenuItem("维修工单", "/repairs", "Tools", "equipment:read"),
                new AuthDtos.MenuItem("巡检任务", "/inspection", "Refresh", "equipment:read"),
                new AuthDtos.MenuItem("保养任务", "/maintenance", "Clock", "equipment:read"),
                new AuthDtos.MenuItem("用户角色", "/users", "User", "user:manage"),
                new AuthDtos.MenuItem("系统配置", "/settings", "Setting", "config:manage")
        ).stream().filter(item -> permissions.contains(item.permission())).toList();
    }
}
