package com.telcoedge.charging;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubscriberLookup {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(SubscriberLookup.class);

    public SubscriberLookup(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }


    @CircuitBreaker(name="subscriberLookup", fallbackMethod = "findSubscriberIdFallback")
    public Long findSubscriberId(String operatorId, String msisdn){
        return jdbcTemplate.query(
                "SELECT id FROM subscribers WHERE operator_id = ? and msisdn = ?" +
                        " AND status = 'ACTIVE'",
                rs->rs.next()?rs.getLong("id"):null,
                operatorId, msisdn
        );
    }

    private Long findSubscriberIdFallback(String operatorId, String msisdn, Exception ex){
        log.warn("Circuit breaker fallback for a subscriber id lookup : operatorId {} , msisdn {}, error {}",
                operatorId, msisdn, ex.getMessage());
        return null;
    }
}
