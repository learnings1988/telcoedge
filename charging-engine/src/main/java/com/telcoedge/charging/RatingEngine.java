package com.telcoedge.charging;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.UsageType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RatingEngine {

    public BigDecimal calculateCharge(Cdr cdr, TariffPlan plan){
        BigDecimal rate = switch( cdr.usageType()){
            case VOICE -> plan.voiceRatePerSecond();
            case DATA -> plan.dataRatePerMb();
            case SMS -> plan.smsRatePerMessage();
        };
        return cdr.quantity().multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCharge(UsageType usageType, BigDecimal quantity,
                                      BigDecimal ratePerUnit){
        return quantity.multiply(ratePerUnit);
    }
}
