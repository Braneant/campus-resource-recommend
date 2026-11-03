package com.campus.recommend.service;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.entity.PasswordResetCredential;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.mapper.PasswordResetCredentialMapper;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.util.PasswordPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {
    private final PasswordResetCredentialMapper credentials;
    private final StudentUserMapper students;
    private final PasswordEncoder passwords;
    private final SmtpPasswordResetDelivery delivery;
    private final Clock clock;
    private final Duration ttl;
    private final Duration cooldown;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetCredentialMapper credentials, StudentUserMapper students,
            PasswordEncoder passwords, SmtpPasswordResetDelivery delivery,
            @Qualifier("passwordResetClock") Clock clock,
            @Value("${security.password-reset.ttl:PT15M}") String ttlValue,
            @Value("${security.password-reset.cooldown:PT1M}") String cooldownValue) {
        Duration ttl = Duration.parse(ttlValue);
        Duration cooldown = Duration.parse(cooldownValue);
        if (ttl.isNegative() || ttl.isZero() || cooldown.isNegative() || cooldown.isZero()) {
            throw new IllegalArgumentException("Password reset TTL and cooldown must be positive");
        }
        this.credentials = credentials;
        this.students = students;
        this.passwords = passwords;
        this.delivery = delivery;
        this.clock = clock;
        this.ttl = ttl;
        this.cooldown = cooldown;
    }

    @Transactional
    public void request(String studentNo, String email) {
        if (!StringUtils.hasText(studentNo) || !StringUtils.hasText(email)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学号和注册邮箱不能为空");
        }
        delivery.requireConfigured();
        StudentUser student = credentials.lockStudentByNumber(studentNo.trim());
        if (!isActive(student) || !email.trim().equalsIgnoreCase(student.getEmail())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学号或注册邮箱不匹配，或账户不可用");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetCredential previous = credentials.selectById(student.getId());
        if (previous != null && previous.getIssuedAt().plus(cooldown).isAfter(now)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码发送过于频繁，请稍后重试");
        }
        byte[] entropy = new byte[32];
        random.nextBytes(entropy);
        String credential = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        PasswordResetCredential next = new PasswordResetCredential();
        next.setStudentId(student.getId());
        next.setTokenHash(hash(credential));
        next.setIssuedAt(now);
        next.setExpiresAt(now.plus(ttl));
        if (previous == null) {
            credentials.insert(next);
        } else {
            credentials.replace(next);
        }
        // 发送失败会回滚本次签发，旧凭证和冷却状态不受影响。
        delivery.send(student.getEmail(), student.getUsername(), credential, ttl);
    }

    public void requireDeliveryConfigured() { delivery.requireConfigured(); }

    @Transactional
    public void reset(String username, String code, String newPassword) {
        PasswordPolicy.validateNewPassword(newPassword);
        if (!StringUtils.hasText(username) || code == null || !code.matches("[A-Za-z0-9_-]{43}")) {
            throw invalidCredential();
        }
        StudentUser student = credentials.lockStudentByUsername(username);
        if (!isActive(student)) {
            throw invalidCredential();
        }
        int consumed = credentials.consume(student.getId(), hash(code), LocalDateTime.now(clock));
        if (consumed != 1) {
            throw invalidCredential();
        }
        StudentUser update = new StudentUser();
        update.setId(student.getId());
        update.setPassword(passwords.encode(newPassword));
        if (students.updateById(update) != 1) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "密码更新失败，请稍后重试");
        }
        // 消费和更新位于同一事务；密码写入失败时凭证仍可重试。
    }

    private boolean isActive(StudentUser student) {
        return student != null && Integer.valueOf(1).equals(student.getStatus());
    }

    private BusinessException invalidCredential() {
        return new BusinessException(ResultCode.PARAM_ERROR, "验证码无效、已过期或账户不可用，请重新获取");
    }

    private String hash(String credential) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(credential.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
}
