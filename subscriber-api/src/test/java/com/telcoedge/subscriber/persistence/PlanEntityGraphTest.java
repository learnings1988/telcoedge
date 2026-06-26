package com.telcoedge.subscriber.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class PlanEntityGraphTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    PlanRepository planRepository;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Statistics enableStats(){
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        return stats;
    }

    @Test
    @Transactional
    void withoutEntityGraph_triggerNPlusOne(){
        Statistics stats = enableStats();

        List<PlanEntity> plans = planRepository.findByOperatorIdAndActiveTrue("acme");
        for(PlanEntity plan : plans){
            plan.getTariffRates().size();
        }

        long queryCount = stats.getPrepareStatementCount();

        assertThat(queryCount).
                as("Expected N+1 queries: 1 for plans + n for rated , got %d", queryCount)
                .isGreaterThan(1);

        stats.setStatisticsEnabled(false);
    }

    @Test
    void withEntityGraph_loadsInSingleQuery(){
        Statistics stats = enableStats();
        List<PlanEntity> plans = planRepository.findByOperatorIdWithRates("acme");
        for(PlanEntity plan : plans){
            plan.getTariffRates().size();
        }

        long queryCount = stats.getPrepareStatementCount();

        assertThat(queryCount).
                as("Expected 1 query with entity graph left join , got %d", queryCount)
                .isEqualTo(1);

        assertThat(plans).isNotEmpty();
        assertThat(plans.get(0).getTariffRates()).hasSize(3);

        stats.setStatisticsEnabled(false);
    }
}
