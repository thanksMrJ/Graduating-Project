package com.ajing.medicalmultimodal.security;

public record JwtPrincipal(Long id, JwtKind kind, String name) {
}
