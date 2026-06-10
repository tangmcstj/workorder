package com.workorder.common;

public record LegacyResponse<T>(int code, String msg, T data) {
    public static <T> LegacyResponse<T> success(String msg, T data) {
        return new LegacyResponse<>(1, msg == null ? "" : msg, data);
    }

    public static <T> LegacyResponse<T> error(String msg) {
        return new LegacyResponse<>(0, msg == null ? "操作失败" : msg, null);
    }
}
