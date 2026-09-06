package dev.aidev.security;

/** Lightweight principal carried in the security context. */
public record AppPrincipal(Long userId, String email, String role) {}
