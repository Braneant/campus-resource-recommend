package com.campus.recommend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class PasswordResetConfig {
    @Bean
    public Clock passwordResetClock() { return Clock.systemUTC(); }

    /** 仅执行 CREATE TABLE IF NOT EXISTS；受限数据库可由 DBA 执行同一迁移并关闭自动建表。 */
    @Bean
    public DataSourceInitializer passwordResetSchema(DataSource dataSource,
            @Value("${security.password-reset.initialize-schema:true}") boolean initialize) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setEnabled(initialize);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V20261102_01__password_reset_credential.sql")));
        return initializer;
    }
}
