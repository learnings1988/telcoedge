package com.telcoedge.charging;

import com.telcoedge.charging.dto.UsageHistoryDto;
import com.telcoedge.charging.persistence.UsageEventEntity;
import com.telcoedge.charging.persistence.UsageEventRepository;

import com.telcoedge.charging.persistence.UsageEventsSpecifications;
import com.telcoedge.domain.UsageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


@Service
public class UsageHistoryService {

    private final UsageEventRepository usageEventRepository;
    private final SubscriberLookup subscriberLookup;

    public UsageHistoryService(UsageEventRepository usageEventRepository, SubscriberLookup subscriberLookup) {
        this.usageEventRepository = usageEventRepository;
        this.subscriberLookup = subscriberLookup;
    }

    @Transactional(readOnly=true)
    public Page<UsageHistoryDto> getHistory (String operatorId, String msisdn,
                                             int page, int size){
        Long subscriberId = subscriberLookup.findSubscriberId(operatorId, msisdn);

        if(subscriberId==null){
            throw new IllegalArgumentException(
                    "subscriber ID not found for " + operatorId + "/"+ msisdn
            );
        }
        Pageable pageable = (Pageable) PageRequest.of(page, size);
        return usageEventRepository.findHistoryBySubscriber(subscriberId, pageable);
    }

    @Transactional(readOnly=true)
    public Page<UsageHistoryDto> getHistoryByType (String operatorId, String msisdn,
                                             String usageType, int page, int size){
        Long subscriberId = subscriberLookup.findSubscriberId(operatorId, msisdn);

        if(subscriberId==null) {
            throw new IllegalArgumentException(
                    "subscriber ID not found for " + operatorId + "/" + msisdn
            );
        }
        Pageable pageable = PageRequest.of(page, size);
        return usageEventRepository.findHistoryBySubscriberIdAndType(subscriberId, usageType , pageable);
    }

    @Transactional(readOnly = true)
    public Page<UsageHistoryDto> getFilteredHistory(String operatorId, String msisdn,
                                                    String usageType, String status,
                                                    Instant from , Instant to ,
                                                    int page , int size) {

        Long subscriberId = subscriberLookup.findSubscriberId(operatorId, msisdn);

        if(subscriberId==null){
            throw new IllegalArgumentException(STR."Subcriber not found: \{operatorId}/\{subscriberId}");
        }

        Specification<UsageEventEntity> spec = Specification.
                where(UsageEventsSpecifications.hasSubscriber(subscriberId));

        if(usageType != null){
            spec = spec.and(UsageEventsSpecifications.hasUsageType(UsageType.valueOf(usageType.toUpperCase())));
        }

        if(status != null){
            spec = spec.and(UsageEventsSpecifications.hasStatus(status));
        }

        if(from != null){
            spec = spec.and(UsageEventsSpecifications.processedAfter(from));
        }

        if(to != null){
            spec = spec.and(UsageEventsSpecifications.processedBefore(to));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "processedAt"));

        Page<UsageEventEntity> entityPage = usageEventRepository.findAll(spec, pageable);
        return entityPage.map(e-> new UsageHistoryDto(
                e.getEventId(),
                e.getUsageType(),
                e.getUnits(),
                e.getRateApplied(),
                e.getAmountCharged(),
                e.getBalanceAfter(),
                e.getProcessedAt()
        ));
    }
}
