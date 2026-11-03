package com.campus.recommend.security;

import com.campus.recommend.annotation.RequireRole;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.entity.Admin;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.interceptor.JwtInterceptor;
import com.campus.recommend.mapper.AdminMapper;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAccountStateTest {
    @Mock JwtUtil jwt;
    @Mock StudentUserMapper students;
    @Mock AdminMapper admins;
    @InjectMocks JwtInterceptor interceptor;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    HandlerMethod studentHandler;
    Claims claims;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer signed-credential");
        response = new MockHttpServletResponse();
        studentHandler = new HandlerMethod(new ProtectedEndpoint(), ProtectedEndpoint.class.getMethod("student"));
        claims = Jwts.claims().subject("learner").add("uid", 7L).add("role", "STUDENT").build();
        when(jwt.parseToken("signed-credential")).thenReturn(claims);
        lenient().when(jwt.getUserId(claims)).thenReturn(7L);
        lenient().when(jwt.getRole(claims)).thenReturn("STUDENT");
        lenient().when(jwt.getUsername(claims)).thenReturn("learner");
    }

    @AfterEach
    void clearContext() { LoginContext.clear(); }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 2})
    @NullSource
    void heldStudentJwtStopsWorkingWhenAccountIsInactive(Integer status) {
        StudentUser student = new StudentUser();
        student.setId(7L);
        student.setUsername("learner");
        student.setStatus(status);
        lenient().when(students.selectById(7L)).thenReturn(student);
        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, studentHandler));
        assertNull(LoginContext.get());
    }

    @Test
    void heldJwtStopsWorkingAfterAccountIsRemoved() {
        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, studentHandler));
        assertNull(LoginContext.get());
    }

    @Test
    void malformedIdentityClaimsAreRejectedAsUnauthorizedAndClearContext() {
        when(jwt.getUserId(claims)).thenThrow(new IllegalArgumentException("malformed identity"));
        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, studentHandler));
        assertNull(LoginContext.get());
    }

    @Test
    void heldAdminJwtStopsWorkingWhenAccountIsDisabled() throws NoSuchMethodException {
        when(jwt.getRole(claims)).thenReturn("ADMIN");
        Admin admin = new Admin();
        admin.setId(7L);
        admin.setUsername("learner");
        admin.setStatus(0);
        lenient().when(admins.selectById(7L)).thenReturn(admin);
        HandlerMethod handler = new HandlerMethod(new ProtectedEndpoint(), ProtectedEndpoint.class.getMethod("admin"));
        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
    }

    @Test
    void roleFailureClearsLoginContextEvenBeforeAfterCompletion() throws NoSuchMethodException {
        StudentUser student = new StudentUser();
        student.setId(7L);
        student.setUsername("learner");
        student.setStatus(1);
        lenient().when(students.selectById(7L)).thenReturn(student);
        HandlerMethod handler = new HandlerMethod(new ProtectedEndpoint(), ProtectedEndpoint.class.getMethod("admin"));
        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
        assertNull(LoginContext.get());
    }

    static class ProtectedEndpoint {
        @RequireRole("STUDENT") public void student() { }
        @RequireRole("ADMIN") public void admin() { }
    }
}
