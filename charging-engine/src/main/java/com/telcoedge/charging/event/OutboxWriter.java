package com.telcoedge.charging.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcoedge.charging.persistence.OutboxEventEntity;
import com.telcoedge.charging.persistence.OutboxEventRepository;
import com.telcoedge.domain.Cdr;
import org.springframework.stereotype.Component;

@Component
public class OutboxWriter {

    public static final String AGGREGATE_TYPE_CDR = "CDR";
    public static final String EVENT_TYPE_CDR_CHARGED = "CDR_CHARGED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueueCharged(Cdr cdr){
        try{
                String payload = objectMapper.writeValueAsString(CdrEvent.fromCdr(cdr));
            OutboxEventEntity row = new OutboxEventEntity(AGGREGATE_TYPE_CDR,
                    cdr.msisdn(),
                    EVENT_TYPE_CDR_CHARGED,
                    payload);
            outboxEventRepository.save(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize CdrEvent for outbox", e);
        }
    }
}
