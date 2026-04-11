package com.ajing.medicalmultimodal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 长度至少 32 字节（UTF-8）");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(JwtPrincipal principal) {
        long ttlMs = properties.getExpireHours() * 3600_000L;
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("kind", principal.kind().name());
        claims.put("name", principal.name());
        return Jwts.builder()
                .claims(claims)
                .subject(principal.kind() + ":" + principal.id())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }

    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String sub = claims.getSubject();
        if (sub == null || !sub.contains(":")) {
            throw new IllegalArgumentException("invalid subject");
        }
        int idx = sub.indexOf(':');
        JwtKind kind = JwtKind.valueOf(sub.substring(0, idx));
        Long id = Long.parseLong(sub.substring(idx + 1));
        String name = claims.get("name", String.class);
        return new JwtPrincipal(id, kind, name != null ? name : "");
    }
}
