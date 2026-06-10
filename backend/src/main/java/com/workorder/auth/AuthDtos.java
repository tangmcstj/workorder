package com.workorder.auth;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;

public class AuthDtos {
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, UserProfile user, List<MenuItem> menus) {
    }

    public record UserProfile(Long id, String username, String displayName, Set<String> roles, Set<String> permissions) {
    }

    public record MenuItem(String title, String path, String icon, String permission) {
    }
}
