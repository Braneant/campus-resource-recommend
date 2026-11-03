package com.campus.recommend.controller;

import com.campus.recommend.context.LoginContext;
import com.campus.recommend.context.LoginUser;
import com.campus.recommend.service.RecommendService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendAuthorizationTest {
    private RecommendController controller;
    private RecommendService recommendations;

    @BeforeEach void setup() {
        controller = new RecommendController();
        recommendations = mock(RecommendService.class);
        ReflectionTestUtils.setField(controller, "recommendService", recommendations);
        LoginUser student = new LoginUser();
        student.setUserId(1L); student.setRole(LoginUser.ROLE_STUDENT);
        LoginContext.set(student);
    }
    @AfterEach void clear() { LoginContext.clear(); }

    @Test void studentCannotReadAnotherUsersRecommendations() {
        assertEquals(1002, controller.personal(2L, 10, List.of()).getCode());
        verifyNoInteractions(recommendations);
    }

    @Test void studentCannotReadAnotherUsersExplanation() {
        assertEquals(1002, controller.explain(2L, 12L).getCode());
        verifyNoInteractions(recommendations);
    }

    @Test void studentMayRequestOwnRecommendationsWithoutAnId() {
        assertEquals(0, controller.personal(null, 10, List.of()).getCode());
        verify(recommendations).personalRecommend(1L, 10, List.of());
    }

    @Test void unauthenticatedExplicitIdIsRejected() {
        LoginContext.clear();
        assertEquals(1002, controller.personal(2L, 10, List.of()).getCode());
        verifyNoInteractions(recommendations);
    }

    @Test void administratorCanRequestAStudentExplicitly() {
        LoginUser admin = new LoginUser();
        admin.setUserId(7L); admin.setRole(LoginUser.ROLE_ADMIN);
        LoginContext.set(admin);
        assertEquals(0, controller.personal(2L, 10, List.of()).getCode());
        verify(recommendations).personalRecommend(2L, 10, List.of());
    }
}
