package com.campus.recommend.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.GlobalExceptionHandler;
import com.campus.recommend.controller.AuthController;
import com.campus.recommend.interceptor.JwtInterceptor;
import com.campus.recommend.service.AuthService;
import com.campus.recommend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.method.HandlerMethod;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringJUnitConfig(PasswordResetIntegrationTest.Fixture.class)
@TestPropertySource(properties = {
        "jwt.secret=isolated-security-test-key-at-least-thirty-two-bytes",
        "jwt.expiration=3600000", "jwt.issuer=security-test",
        "spring.mail.host=smtp.test.invalid",
        "security.password-reset.mail-from=no-reply@example.edu",
        "security.password-reset.ttl=PT15M", "security.password-reset.cooldown=PT1M"
})
@ExtendWith(OutputCaptureExtension.class)
class PasswordResetIntegrationTest {
    @Autowired AuthService auth;
    @Autowired AuthController controller;
    @Autowired JwtInterceptor interceptor;
    @Autowired JdbcTemplate database;
    @Autowired DataSource dataSource;
    @Autowired MutableClock clock;
    @Autowired TestPasswords passwords;
    @Autowired Mailbox mailbox;
    @Autowired com.campus.recommend.service.ResetDispatch dispatch;
    private MockMvc mvc;
    private static final java.util.concurrent.atomic.AtomicInteger CLIENTS = new java.util.concurrent.atomic.AtomicInteger();
    private String client;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void isolatedUsers() {
        client = "fixture-" + CLIENTS.incrementAndGet();
        new ResourceDatabasePopulator(new ClassPathResource("security-schema.sql")).execute(dataSource);
        database.update("DELETE FROM password_reset_credential");
        database.update("DELETE FROM student_user");
        database.update("DELETE FROM admin");
        passwords.failNextEncode = false;
        database.update("INSERT INTO student_user (id, student_no, username, password, email, status) VALUES (7, '20260007', 'learner', ?, 'learner@example.edu', 1)",
                passwords.encode("original-password"));
        database.update("INSERT INTO student_user (id, student_no, username, password, email, status) VALUES (8, '20260008', 'other', ?, 'other@example.edu', 1)",
                passwords.encode("other-original-password"));
        database.update("INSERT INTO admin (id, username, password, status) VALUES (9, 'manager', ?, 1)", passwords.encode("admin-password"));
        clock.now = Instant.parse("2026-11-02T12:00:00Z");
        mailbox.messages.clear();
        mailbox.lastAttempt = null;
        mailbox.fail = false;
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void issuesRandomCredentialOnlyByEmailAndStoresItsSha256Hash() throws Exception {
        String response = forgotResponse("20260007", "learner@example.edu");
        assertEquals(0, code(response));
        String credential = deliveredCredential();
        assertFalse(response.contains(credential));
        assertArrayEquals(new String[]{"learner@example.edu"}, mailbox.messages.get(0).getTo());
        assertEquals("no-reply@example.edu", mailbox.messages.get(0).getFrom());
        String storedHash = database.queryForObject("SELECT token_hash FROM password_reset_credential WHERE student_id=7", String.class);
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(credential.getBytes(StandardCharsets.US_ASCII)));
        assertEquals(expectedHash, storedHash);
        assertNotEquals(credential, storedHash);
        assertEquals(LocalDateTime.of(2026, 11, 2, 12, 15), database.queryForObject(
                "SELECT expires_at FROM password_reset_credential WHERE student_id=7", LocalDateTime.class));
    }

    @Test
    void invalidCredentialCannotWritePassword() throws Exception {
        String response = resetResponse("learner", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "replacement-password");
        assertEquals(1001, code(response));
        assertOriginalPassword();
    }

    @Test
    void credentialExpiresAtItsDeadline() throws Exception {
        String credential = issue();
        clock.now = clock.now.plus(Duration.ofMinutes(15));
        assertEquals(1001, code(resetResponse("learner", credential, "replacement-password")));
        assertOriginalPassword();
    }

    @Test
    void credentialCanBeUsedExactlyOnce() throws Exception {
        String credential = issue();
        assertEquals(0, code(resetResponse("learner", credential, "first-replacement")));
        assertEquals(1001, code(resetResponse("learner", credential, "second-replacement")));
        assertTrue(passwords.matches("first-replacement", storedPassword()));
    }

    @Test
    void concurrentConsumersHaveOnlyOneWinner() throws Exception {
        String credential = issue();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> consume = () -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return code(resetResponse("learner", credential, "concurrent-replacement"));
            };
            Future<Integer> first = pool.submit(consume);
            Future<Integer> second = pool.submit(consume);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<Integer> responses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, responses.stream().filter(value -> value == 0).count());
            assertEquals(1, responses.stream().filter(value -> value == 1001).count());
            assertTrue(passwords.matches("concurrent-replacement", storedPassword()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void issueCooldownAndReplacementInvalidateThePreviousCredential() throws Exception {
        String first = issue();
        assertEquals(0, code(forgotResponse("20260007", "learner@example.edu")));
        assertEquals(1, mailbox.messages.size());
        clock.now = clock.now.plusSeconds(61);
        String second = issue();
        assertNotEquals(first, second);
        assertEquals(1001, code(resetResponse("learner", first, "replacement-password")));
        assertEquals(0, code(resetResponse("learner", second, "replacement-password")));
        assertEquals(1, database.queryForObject("SELECT COUNT(*) FROM password_reset_credential WHERE student_id=7", Integer.class));
    }

    @Test
    void credentialIsBoundToTheStudentAndCannotResetAnotherStudentOrAdmin() throws Exception {
        String credential = issue();
        assertEquals(1001, code(resetResponse("other", credential, "replacement-password")));
        assertEquals(1001, code(resetResponse("manager", credential, "replacement-password")));
        assertEquals(0, code(resetResponse("learner", credential, "replacement-password")));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void disablingOrDeletingAccountPreventsBothIssueAndUse(int status) throws Exception {
        String credential = issue();
        database.update("UPDATE student_user SET status=? WHERE id=7", status);
        assertEquals(1001, code(resetResponse("learner", credential, "replacement-password")));
        assertEquals(0, code(forgotResponse("20260007", "learner@example.edu")));
        assertOriginalPassword();
        assertEquals(1, mailbox.messages.size());
    }

    @Test
    void mismatchEmailCannotReceiveAnyCredential() throws Exception {
        assertEquals(0, code(forgotResponse("20260007", "attacker@example.edu")));
        assertTrue(mailbox.messages.isEmpty());
        assertEquals(0, database.queryForObject("SELECT COUNT(*) FROM password_reset_credential", Integer.class));
    }

    @Test
    void sameResponseForKnownAndUnknownAccounts() throws Exception {
        String known = forgotResponse("20260007", "learner@example.edu");
        String unknown = forgotResponse("29999999", "unknown@example.edu");
        assertEquals(known, unknown);
        assertEquals(1, mailbox.messages.size());
    }

    @Test
    void deliveryFailureRollsBackAndDoesNotLeakCredentialInResponseOrLogs(CapturedOutput output) throws Exception {
        mailbox.fail = true;
        String response = forgotResponse("20260007", "learner@example.edu");
        assertEquals(0, code(response));
        assertNotNull(mailbox.lastAttempt);
        String credential = credentialIn(mailbox.lastAttempt);
        assertFalse(response.contains(credential));
        assertFalse(output.getAll().contains(credential));
        assertEquals(0, database.queryForObject("SELECT COUNT(*) FROM password_reset_credential", Integer.class));
    }

    @Test
    void passwordWriteFailureRollsBackCredentialConsumption() throws Exception {
        String credential = issue();
        passwords.failNextEncode = true;
        assertEquals(5000, code(resetResponse("learner", credential, "replacement-password")));
        assertOriginalPassword();
        assertEquals(0, code(resetResponse("learner", credential, "replacement-password")));
    }

    @Test
    void successfulResetRevokesHeldJwtAndNewLoginWorks() throws Exception {
        String token = auth.login("learner", "original-password").getToken();
        assertTrue(checkToken(token));
        String credential = issue();
        assertEquals(0, code(resetResponse("learner", credential, "replacement-password")));
        assertThrows(BusinessException.class, () -> checkToken(token));
        assertTrue(checkToken(auth.login("learner", "replacement-password").getToken()));
    }

    private boolean checkToken(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer " + token);
        HandlerMethod handler = new HandlerMethod(new JwtAccountStateTest.ProtectedEndpoint(),
                JwtAccountStateTest.ProtectedEndpoint.class.getMethod("student"));
        try {
            return interceptor.preHandle(request, new MockHttpServletResponse(), handler);
        } finally {
            com.campus.recommend.context.LoginContext.clear();
        }
    }

    private String issue() throws Exception {
        assertEquals(0, code(forgotResponse("20260007", "learner@example.edu")));
        return deliveredCredential();
    }

    private String deliveredCredential() {
        assertFalse(mailbox.messages.isEmpty(), "a successful request must actually deliver a credential");
        return credentialIn(mailbox.messages.get(mailbox.messages.size() - 1));
    }

    private String credentialIn(SimpleMailMessage message) {
        Matcher matcher = Pattern.compile("(?<![A-Za-z0-9_-])[A-Za-z0-9_-]{43}(?![A-Za-z0-9_-])").matcher(message.getText());
        assertTrue(matcher.find(), "mail must carry a full 256-bit URL-safe credential");
        return matcher.group();
    }

    private String forgotResponse(String studentNo, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/forgot-password").with(request -> { request.setRemoteAddr(client); return request; }).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("studentNo", studentNo, "email", email))))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(dispatch.awaitIdle(Duration.ofSeconds(5)));
        return response;
    }

    private String resetResponse(String username, String credential, String password) throws Exception {
        return mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "code", credential, "newPassword", password))))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int code(String response) throws Exception { return json.readTree(response).path("code").asInt(); }
    private String storedPassword() { return database.queryForObject("SELECT password FROM student_user WHERE id=7", String.class); }
    private void assertOriginalPassword() { assertTrue(passwords.matches("original-password", storedPassword())); }

    static class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-11-02T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    static class Mailbox {
        final List<SimpleMailMessage> messages = new CopyOnWriteArrayList<>();
        volatile SimpleMailMessage lastAttempt;
        volatile boolean fail;
    }

    static class TestPasswords implements PasswordEncoder {
        final PasswordEncoder delegate = new BCryptPasswordEncoder(4);
        volatile boolean failNextEncode;
        @Override public String encode(CharSequence rawPassword) {
            if (failNextEncode) { failNextEncode = false; throw new IllegalStateException("isolated password writer failure"); }
            return delegate.encode(rawPassword);
        }
        @Override public boolean matches(CharSequence rawPassword, String encodedPassword) { return delegate.matches(rawPassword, encodedPassword); }
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan("com.campus.recommend.mapper")
    @Import({AuthService.class, AuthController.class, JwtUtil.class, JwtInterceptor.class})
    @ComponentScan(basePackages = "com.campus.recommend.service", useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                    pattern = "com\\.campus\\.recommend\\.service\\.(PasswordResetService|SmtpPasswordResetDelivery|ResetDispatch)"))
    static class Fixture {
        @Bean DataSource dataSource() {
            JdbcDataSource source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:account_security;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
            return source;
        }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setLogImpl(NoLoggingImpl.class);
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setConfiguration(configuration);
            return factory.getObject();
        }
        @Bean JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean TestPasswords passwordEncoder() { return new TestPasswords(); }
        @Bean(name = "passwordResetClock") MutableClock clock() { return new MutableClock(); }
        @Bean Mailbox mailbox() { return new Mailbox(); }
        @Bean JavaMailSender mailSender(Mailbox mailbox) {
            JavaMailSender sender = mock(JavaMailSender.class);
            doAnswer(invocation -> {
                SimpleMailMessage message = new SimpleMailMessage(invocation.getArgument(0, SimpleMailMessage.class));
                mailbox.lastAttempt = message;
                if (mailbox.fail) { throw new MailSendException("transport rejected " + message.getText()); }
                mailbox.messages.add(message);
                return null;
            }).when(sender).send(any(SimpleMailMessage.class));
            return sender;
        }
    }
}
