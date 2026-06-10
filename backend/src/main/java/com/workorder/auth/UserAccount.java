package com.workorder.auth;

import java.util.Set;

public record UserAccount(Long id, String username, String displayName, String passwordHash, Set<String> roles, Set<String> permissions) {
}
