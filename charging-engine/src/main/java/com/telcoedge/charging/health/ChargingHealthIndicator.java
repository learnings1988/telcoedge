package com.telcoedge.charging.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class ChargingHealthIndicator  implements HealthIndicator {

    private final DataSource dataSource;

    public ChargingHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health(){
        try(Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select count(*) from balances where amount<0")){

            rs.next();
            long negativeBalances = rs.getLong(1);

            if(negativeBalances>0){
                return Health.down().withDetail("reason" , "negative balances detected")
                        .withDetail("count" , negativeBalances)
                        .build();
            }

            return Health.up().withDetail("negative Balances" , 0).build();
        }catch (Exception exp){
            return Health.down().withDetail("reason", "cannot verify balance integrity")
                    .withException(exp)
                    .build();
        }
    }
}
