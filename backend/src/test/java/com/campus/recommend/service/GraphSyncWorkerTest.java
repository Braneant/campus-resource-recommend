package com.campus.recommend.service;

import com.campus.recommend.config.Neo4jTemplate;
import com.campus.recommend.entity.LearningResource;
import com.campus.recommend.entity.Neo4jSyncLog;
import com.campus.recommend.mapper.LearningResourceMapper;
import com.campus.recommend.mapper.Neo4jSyncLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.*;
import java.util.Collections;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GraphSyncWorkerTest {
    private final Neo4jTemplate graph = mock(Neo4jTemplate.class);
    private final Neo4jSyncLogMapper logs = mock(Neo4jSyncLogMapper.class);
    private final LearningResourceMapper resources = mock(LearningResourceMapper.class);
    private final GraphSyncWorker worker = new GraphSyncWorker();
    private Neo4jSyncLog entry;
    private LearningResource resource;

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(worker, "graph", graph);
        ReflectionTestUtils.setField(worker, "logs", logs);
        ReflectionTestUtils.setField(worker, "resources", resources);
        entry = new Neo4jSyncLog(); entry.setId(7L); entry.setResourceId(1L); entry.setSyncStatus(0); entry.setSyncType(1);
        resource = new LearningResource(); resource.setId(1L); resource.setResourceNo("FIXTURE"); resource.setTitle("Test"); resource.setStatus(1);
        when(logs.lockById(7L)).thenReturn(entry);
        when(resources.lockById(1L)).thenReturn(resource);
    }

    @Test void failureIsVisibleAndRetrySucceedsExactlyOnce() {
        doThrow(new IllegalStateException("simulated offline")).doNothing().when(graph).runWrite(anyString(), anyMap());
        worker.retry(7L);
        assertEquals(2, entry.getSyncStatus()); assertNotNull(entry.getErrorMsg());
        verify(logs).update(isNull(), any());
        worker.retry(7L);
        assertEquals(1, entry.getSyncStatus()); assertNull(entry.getErrorMsg());
        worker.retry(7L);
        verify(graph, times(2)).runWrite(anyString(), anyMap());
    }

    @Test void retryOfOldApprovalUsesCurrentRejectedState() {
        resource.setStatus(2);
        worker.retry(7L);
        verify(graph).runWrite(contains("DETACH DELETE"), anyMap());
        assertEquals(1, entry.getSyncStatus());
    }

    @Test void missingKnowledgePointIsRecordedAsFailure() {
        resource.setPrimaryKpId(999L);
        when(graph.runRead(anyString(), anyMap())).thenReturn(java.util.List.of(java.util.Map.of("count", 0L)));
        worker.retry(7L);
        assertEquals(2, entry.getSyncStatus());
        verify(graph, never()).runWrite(anyString(), anyMap());
    }

    @Test void concurrentRejectionCannotBeOverwrittenBySlowApproval() throws Exception {
        JdbcDataSource db = new JdbcDataSource();
        db.setURL("jdbc:h2:mem:graph_interleaving;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        JdbcTemplate jdbc = new JdbcTemplate(db);
        jdbc.execute("CREATE TABLE IF NOT EXISTS learning_resource (id BIGINT PRIMARY KEY, status INT)");
        jdbc.update("DELETE FROM learning_resource");
        jdbc.update("INSERT INTO learning_resource VALUES (1,1)");
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(db));
        Neo4jSyncLog rejection = new Neo4jSyncLog();
        rejection.setId(8L); rejection.setResourceId(1L); rejection.setSyncType(3); rejection.setSyncStatus(0);
        when(logs.lockById(8L)).thenReturn(rejection);
        when(resources.lockById(1L)).thenAnswer(call -> {
            Integer status = jdbc.queryForObject("SELECT status FROM learning_resource WHERE id=1 FOR UPDATE", Integer.class);
            LearningResource current = new LearningResource();
            current.setId(1L); current.setResourceNo("FIXTURE"); current.setTitle("Test"); current.setStatus(status);
            return current;
        });
        CountDownLatch graphEntered = new CountDownLatch(1), finishApproval = new CountDownLatch(1), rejectionStarted = new CountDownLatch(1);
        var operations = Collections.synchronizedList(new ArrayList<String>());
        doAnswer(call -> {
            String cypher = call.getArgument(0);
            if (cypher.contains("MERGE (r:Resource")) {
                graphEntered.countDown();
                assertTrue(finishApproval.await(3, TimeUnit.SECONDS));
                operations.add("approve");
            } else if (cypher.contains("DETACH DELETE")) operations.add("reject");
            return null;
        }).when(graph).runWrite(anyString(), anyMap());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> approval = pool.submit(() -> tx.executeWithoutResult(s -> worker.retry(7L)));
            assertTrue(graphEntered.await(3, TimeUnit.SECONDS));
            Future<?> later = pool.submit(() -> tx.executeWithoutResult(s -> {
                rejectionStarted.countDown();
                jdbc.update("UPDATE learning_resource SET status=2 WHERE id=1");
                worker.retry(8L);
            }));
            assertTrue(rejectionStarted.await(3, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> later.get(200, TimeUnit.MILLISECONDS));
            finishApproval.countDown();
            approval.get(4, TimeUnit.SECONDS);
            later.get(4, TimeUnit.SECONDS);
            assertEquals(java.util.List.of("approve", "reject"), operations);
        } finally {
            finishApproval.countDown(); pool.shutdownNow();
        }
    }
}
