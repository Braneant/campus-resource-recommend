package com.campus.recommend.security;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.context.LoginUser;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordSafetyConfigTest {
    @AfterEach void clear() { LoginContext.clear(); }

    @Test void changePasswordRejectsShortAndOverlongUtf8() {
        UserService service = new UserService();
        StudentUserMapper students = mock(StudentUserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ReflectionTestUtils.setField(service, "studentUserMapper", students);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        LoginUser login = new LoginUser(); login.setUserId(7L); login.setRole("STUDENT"); LoginContext.set(login);
        StudentUser user = new StudentUser(); user.setId(7L); user.setPassword("stored-hash");
        when(students.selectById(7L)).thenReturn(user);
        when(encoder.matches("old", "stored-hash")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.changePassword("old", "short"));
        assertThrows(BusinessException.class, () -> service.changePassword("old", "界".repeat(25)));
        verify(students, never()).updateById(any(StudentUser.class));
    }

    @Test void smtpConnectionReadAndWriteTimeoutsAreFiniteByDefault() throws IOException {
        var source = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml")).get(0);
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withPropertyValues("spring.mail.host=localhost")
            .withInitializer(context -> context.getEnvironment().getPropertySources().addLast(source))
            .run(context -> {
                var props = context.getBean(JavaMailSenderImpl.class).getJavaMailProperties();
                for (String name : new String[]{"connectiontimeout", "timeout", "writetimeout"}) {
                    String value = props.getProperty("mail.smtp." + name);
                    assertNotNull(value, name);
                    assertTrue(Integer.parseInt(value) > 0 && Integer.parseInt(value) <= 30000, name);
                }
            });
    }
}
