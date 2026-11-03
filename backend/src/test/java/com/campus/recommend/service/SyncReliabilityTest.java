package com.campus.recommend.service;

import com.campus.recommend.config.Neo4jTemplate;
import com.campus.recommend.entity.LearningResource;
import com.campus.recommend.entity.Neo4jSyncLog;
import com.campus.recommend.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SyncReliabilityTest {
    @Test void auditDoesNotContactGraphBeforeMysqlCommit() {
        Neo4jSyncService sync = new Neo4jSyncService();
        Neo4jTemplate graph = mock(Neo4jTemplate.class);
        Neo4jSyncLogMapper logs = mock(Neo4jSyncLogMapper.class);
        ReflectionTestUtils.setField(sync, "neo4jTemplate", graph);
        ReflectionTestUtils.setField(sync, "syncLogMapper", logs);
        LearningResource resource = resource();
        TransactionSynchronizationManager.initSynchronization();
        try {
            sync.syncResource(resource, 1);
            verify(logs).insert(any(Neo4jSyncLog.class));
            verifyNoInteractions(graph);
            // A rollback does not run afterCommit. Clearing synchronization simulates it.
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test void auditNeedsDatabaseTransaction() throws Exception {
        assertNotNull(ResourceAuditService.class.getMethod("approve", Long.class, Long.class, Long.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertNotNull(ResourceAuditService.class.getMethod("reject", Long.class, String.class, Long.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test void reviewInsertFailureActuallyRollsBackResourceUpdate() {
        JdbcDataSource datasource = new JdbcDataSource();
        datasource.setURL("jdbc:h2:mem:audit-rollback;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(datasource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS audit_fixture(id BIGINT PRIMARY KEY,status INT)");
        jdbc.update("DELETE FROM audit_fixture");
        jdbc.update("INSERT INTO audit_fixture VALUES(1,0)");
        LearningResourceMapper resources = mock(LearningResourceMapper.class);
        ResourceReviewRecordMapper reviews = mock(ResourceReviewRecordMapper.class);
        Neo4jSyncService sync = mock(Neo4jSyncService.class);
        when(resources.selectById(1L)).thenReturn(resource());
        when(resources.updateById(any(LearningResource.class))).thenAnswer(invocation ->
                jdbc.update("UPDATE audit_fixture SET status=1 WHERE id=1"));
        when(reviews.insert(any(com.campus.recommend.entity.ResourceReviewRecord.class)))
                .thenThrow(new IllegalStateException("Injected review storage failure"));
        ResourceAuditService target = new ResourceAuditService();
        ReflectionTestUtils.setField(target, "resourceMapper", resources);
        ReflectionTestUtils.setField(target, "reviewRecordMapper", reviews);
        ReflectionTestUtils.setField(target, "neo4jSyncService", sync);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(datasource),
                new AnnotationTransactionAttributeSource()));
        ResourceAuditService service = (ResourceAuditService) proxy.getProxy();
        assertThrows(IllegalStateException.class, () -> service.approve(1L, null, 1L));
        assertEquals(0, jdbc.queryForObject("SELECT status FROM audit_fixture WHERE id=1", Integer.class));
        verifyNoInteractions(sync);
    }

    private LearningResource resource() {
        LearningResource r = new LearningResource();
        r.setId(1L); r.setResourceNo("RES-TEST"); r.setTitle("Fixture"); r.setStatus(1);
        return r;
    }
}
