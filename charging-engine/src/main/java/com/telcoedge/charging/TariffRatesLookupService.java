package com.telcoedge.charging;

import com.telcoedge.charging.persistence.TariffRateRepository;
import com.telcoedge.charging.persistence.TariffRateView;
import com.telcoedge.domain.UsageType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TariffRatesLookupService {

    private final TariffRateRepository tariffRateRepository;

    public TariffRatesLookupService(TariffRateRepository tariffRateRepository) {
        this.tariffRateRepository = tariffRateRepository;
    }

    @Cacheable(value = "tariffRates" , key = "#planId + '-' + #usageType")
    public Optional<TariffRateView> findRate(Long planId, UsageType usageType){
        return tariffRateRepository.findByPlanIdAndUsageType(planId, usageType);
    }
}
