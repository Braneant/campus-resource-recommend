package com.campus.recommend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * JWT 工具类（jjwt 0.12.x 新版 API）
 * 载荷中存放：uid（用户id）、role（STUDENT/ADMIN）、username
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration:86400000}")
    private long expiration;
    @Value("${jwt.issuer:campus-recommend}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // HS256 要求密钥至少 256bit(32字节)，不足则补全
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** 生成 token */
    public String generateToken(Long userId, String role, String username) {
        return generateToken(userId, role, username, null);
    }

    public String passwordVersion(String passwordHash) {
        if (passwordHash == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(passwordHash.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
    }

    public String generateToken(Long userId, String role, String username, String passwordHash) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .claim("uid", userId)
                .claim("role", role)
                .claim("pv", passwordVersion(passwordHash))
                .signWith(key)
                .compact();
    }

    /** 解析 token，失败时抛出 BusinessException(UNAUTHORIZED) */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new com.campus.recommend.common.BusinessException(
                    com.campus.recommend.common.ResultCode.UNAUTHORIZED, "token 无效或已过期");
        }
    }

    public Long getUserId(Claims claims) {
        return claims.get("uid", Long.class);
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getUsername(Claims claims) {
        return claims.getSubject();
    }
}
