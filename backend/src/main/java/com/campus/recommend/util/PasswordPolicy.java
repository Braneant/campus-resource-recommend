package com.campus.recommend.util;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/** BCrypt 只处理最多72字节；拒绝超限输入，避免截断成同一密码。 */
public final class PasswordPolicy {
    private PasswordPolicy() { }

    public static void validateNewPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码须为至少8位且不超过72字节");
        }
    }
}
