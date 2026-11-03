package com.campus.recommend.security;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.GlobalExceptionHandler;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.controller.AuthController;
import com.campus.recommend.dto.RegisterDTO;
import com.campus.recommend.entity.Admin;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.mapper.AdminMapper;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.service.AuthService;
import com.campus.recommend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
class AuthServiceSecurityTest {
    @Mock StudentUserMapper students;
    @Mock AdminMapper admins;
    @Mock PasswordEncoder passwords;
    @Mock JwtUtil jwt;
    @InjectMocks AuthService auth;

    private StudentUser student;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        student = new StudentUser();
        student.setId(7L);
        student.setUsername("learner");
        student.setPassword("stored-password-hash");
        student.setStudentNo("20260007");
        student.setEmail("learner@example.edu");
        student.setStatus(1);
        lenient().when(passwords.matches(anyString(), anyString())).thenReturn(true);
        lenient().when(passwords.encode(anyString())).thenReturn("new-stored-hash");
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", auth);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 2})
    @NullSource
    void studentLoginRequiresExactlyActiveStatus(Integer status) {
        student.setStatus(status);
        when(students.selectOne(any())).thenReturn(student);
        BusinessException failure = assertThrows(BusinessException.class,
                () -> auth.login("learner", "correct-password"));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), failure.getCode());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 2})
    @NullSource
    void adminLoginRequiresExactlyActiveStatus(Integer status) {
        Admin admin = new Admin();
        admin.setId(9L);
        admin.setUsername("manager");
        admin.setPassword("stored-admin-hash");
        admin.setStatus(status);
        when(students.selectOne(any())).thenReturn(null);
        when(admins.selectOne(any())).thenReturn(admin);
        assertThrows(BusinessException.class, () -> auth.login("manager", "correct-password"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijk", "汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉汉"})
    void registerRejectsPasswordsOutsideEightToSeventyTwoUtf8Bytes(String password) {
        RegisterDTO request = new RegisterDTO();
        request.setUsername("new-learner");
        request.setStudentNo("20269999");
        request.setPassword(password);
        assertThrows(BusinessException.class, () -> auth.register(request));
    }

    @Test
    void resetEndpointRejectsMissingCredentialBeforeWritingPassword() throws Exception {
        lenient().when(students.selectOne(any())).thenReturn(student);
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"learner\",\"newPassword\":\"a-secure-new-password\"}"))
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void forgotEndpointRejectsMissingEmail() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNo\":\"20260007\"}"))
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()));
    }
}
