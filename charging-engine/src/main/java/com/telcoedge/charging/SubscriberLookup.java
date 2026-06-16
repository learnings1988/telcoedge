package com.telcoedge.charging;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubscriberLookup {

    private final JdbcTemplate jdbcTemplate;

    public SubscriberLookup(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findSubscriberId(String operatorId, String msisdn){
        return jdbcTemplate.query(
                "SELECT id FROM subscribers WHERE operator_id = ? and msisdn = ?" +
                        " AND status = 'ACTIVE'",
                rs->rs.next()?rs.getLong("id"):null,
                operatorId, msisdn
        );
    }
}
