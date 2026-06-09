package com.telcoedge.charging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.telcoedge.domain.ChargingEvent;
import com.telcoedge.domain.ChargingEvent.*;
import java.math.BigDecimal;

public class ChargingEventLogger {

    private static final Logger log = LoggerFactory.getLogger( ChargingEventLogger.class);
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("500");

    public String describeEvent(ChargingEvent event){

        return switch (event){
          case Charged c when c.amount().compareTo(HIGH_VALUE_THRESHOLD)>0 ->
          "High value charge: " + c.amount() + " on " + c.msisdn() + " Remaining Balance " +
          c.remainingBalance();

          case Charged c
              -> "charged: " + c.amount() + " on " + c.msisdn() + " Remaining Balance " +
                  c.remainingBalance();

          case Failed f
                -> "Failed [" + f.reason() + "]: amount attempted " + f.attemptedAmount()
                    + " on " + f.msisdn();

            case Duplicate d
                -> "Duplicate of : " + d.originalEventId() + " on msisdn: " + d.msisdn();
        };

    }
}
