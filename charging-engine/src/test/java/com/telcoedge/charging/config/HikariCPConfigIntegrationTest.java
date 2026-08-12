package com.telcoedge.charging.config;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import javax.sql.DataSource;
import java.sql.Connection;

@Testcontainers
@SpringBootTest
public class HikariCPConfigIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url" , postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.pool-name", ()->"charging-engine-pool");
        registry.add("spring.datasource.hikari.maximum-pool-size", ()->20);
        registry.add("spring.datasource.hikari.minimum-idle", ()->5);
        registry.add("spring.datasource.hikari.connection-timeout", ()->3000);
    }

    @Autowired
    DataSource dataSource;

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void hikariPoolUsesConfiguredSize(){
        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertThat(hikari.getPoolName()).isEqualTo("charging-engine-pool");
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(20);
        assertThat(hikari.getMinimumIdle()).isEqualTo(5);
        assertThat(hikari.getConnectionTimeout()).isEqualTo(3000);
    }

    @Test
    void poolMetricsRegisteredAfterConnectionUse() throws Exception{

        try(Connection connection = dataSource.getConnection()){
            assertThat(connection.isValid(1)).isTrue();
        }

        assertThat(meterRegistry.find("hikaricp.connections.max")
                .tag("pool","charging-engine-pool")
                .gauge()).isNotNull();

        assertThat(meterRegistry.find("hikaricp.connections.active")
                .tag("pool","charging-engine-pool")
                .gauge()).isNotNull();
    }
}
