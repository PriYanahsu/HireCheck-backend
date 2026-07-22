package com.hirecheck.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtil {
    private ClientIpUtil() {}
    public static String getClientIp(HttpServletRequest request) {
        String ip = first(request.getHeader("CF-Connecting-IP"));
        if (ip == null) ip = first(request.getHeader("X-Real-IP"));
        if (ip == null) ip = first(request.getHeader("X-Forwarded-For"));
        if (ip == null) ip = request.getRemoteAddr();
        return ip != null ? (ip.startsWith("::ffff:") ? ip.substring(7) : ip) : "unknown";
    }
    private static String first(String v) {
        if (v == null || v.isBlank()) return null;
        String ip = v.split(",")[0].trim();
        return ip.isEmpty() ? null : ip;
    }
}
