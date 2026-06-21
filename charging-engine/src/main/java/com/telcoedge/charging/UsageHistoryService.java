package com.telcoedge.charging;

import com.telcoedge.charging.dto.UsageHistoryDto;
import com.telcoedge.charging.persistence.UsageEventRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



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
}
