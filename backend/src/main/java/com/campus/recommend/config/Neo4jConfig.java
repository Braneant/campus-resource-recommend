package com.campus.recommend.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Neo4j 驱动配置（使用官方 neo4j-java-driver 直接驱动）
 * 仅创建 Driver 对象，首次执行查询时才真正建立连接，因此不依赖 Neo4j 是否启动也能完成 Spring 上下文装配。
 */
@Configuration
public class Neo4jConfig {

    @Value("${neo4j.uri:bolt://localhost:7687}")
    private String uri;
    @Value("${neo4j.username:neo4j}")
    private String username;
    @Value("${neo4j.password:neo4j123}")
    private String password;
    @Value("${neo4j.database:neo4j}")
    private String database;

    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password),
                Config.builder()
                        .withConnectionTimeout(10, TimeUnit.SECONDS)
                        .withMaxConnectionPoolSize(20)
                        .build());
    }

    @Bean
    public String neo4jDatabase() {
        return database;
    }
}
