package com.campus.recommend.service;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/** 唯一投递出口是 SMTP；未配置或发送失败均明确失败，禁止打印邮件或返回凭证。 */
@Service
public class SmtpPasswordResetDelivery {
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String host;
    private final String from;

    public SmtpPasswordResetDelivery(ObjectProvider<JavaMailSender> mailSenders,
            @Value("${spring.mail.host:}") String host,
            @Value("${security.password-reset.mail-from:}") String from) {
        this.mailSenders = mailSenders;
        this.host = host;
        this.from = from;
    }

    public void requireConfigured() {
        if (!StringUtils.hasText(host) || !StringUtils.hasText(from) || mailSenders.getIfAvailable() == null) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "邮件服务未配置，暂时无法找回密码，请联系管理员");
        }
    }

    public void send(String email, String username, String credential, Duration ttl) {
        requireConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("校园学习资源平台 · 密码重置验证码");
        message.setText("账户：" + username + "\n\n密码重置验证码：\n" + credential
                + "\n\n请在" + ttl.toMinutes() + "分钟内使用，仅能使用一次。"
                + "\n如果不是您本人发起，请忽略此邮件，勿向他人提供验证码。");
        try {
            mailSenders.getObject().send(message);
        } catch (MailException failure) {
            // SMTP 异常可能包含整封邮件及验证码，不向日志/响应传递异常或 cause。
            throw new BusinessException(ResultCode.SERVER_ERROR, "验证码邮件发送失败，请稍后重试或联系管理员");
        }
    }
}
