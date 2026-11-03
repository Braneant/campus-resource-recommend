package com.campus.recommend.security;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JwtIssuerTest {
    @Test
    void passwordChangeChangesSignedCredentialVersion() {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "isolated-issuer-test-secret-at-least-thirty-two-bytes");
        ReflectionTestUtils.setField(jwt, "issuer", "campus-recommend");
        ReflectionTestUtils.setField(jwt, "expiration", 60000L);
        jwt.init();
        String token = jwt.generateToken(7L, "STUDENT", "learner", "old-password-hash");
        String version = jwt.parseToken(token).get("pv", String.class);
        assertEquals(jwt.passwordVersion("old-password-hash"), version);
        assertNotEquals(jwt.passwordVersion("new-password-hash"), version);
    }
    @Test
    void tokenFromAnotherIssuerCannotBeUsedEvenIfKeyWasShared() {
        String secret = "isolated-issuer-test-secret-at-least-thirty-two-bytes";
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", secret);
        ReflectionTestUtils.setField(jwt, "issuer", "campus-recommend");
        jwt.init();
        String token = Jwts.builder().issuer("another-application").subject("learner")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60000))
                .claim("uid", 7L).claim("role", "STUDENT")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        assertThrows(BusinessException.class, () -> jwt.parseToken(token));
    }
}
