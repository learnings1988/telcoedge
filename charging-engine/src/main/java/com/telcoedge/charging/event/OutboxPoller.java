package com.telcoedge.charging.event;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcoedge.charging.persistence.OutboxEventEntity;
import com.telcoedge.charging.persistence.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventRepository outboxEventRepository;
    private final CdrEventPublisher cdrEventPublisher;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final boolean enabled;

    public OutboxPoller(OutboxEventRepository outboxEventRepository,
                        CdrEventPublisher cdrEventPublisher,
                        ObjectMapper objectMapper,
                        @Value("${telcoedge.outbox.poller.batch-size:50}") int batchSize,
                        @Value("${telcoedge.outbox.poller.enabled:true}") boolean enabled) {
        this.outboxEventRepository = outboxEventRepository;
        this.cdrEventPublisher = cdrEventPublisher;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.enabled = enabled;
    }

    @org.springframework.scheduling.annotation.Scheduled(
            fixedDelayString = "${telcoedge.outbox.poller.fixed-delay-ms:1000}"
    )
    @Transactional
    public void scheduledPublish(){
        if(!enabled){
            return;
        }
        drainBatch();
    }


    @Transactional
    public void publishBatch(){
        drainBatch();
    }

    private void drainBatch(){
        List<OutboxEventEntity> batch = outboxEventRepository.findUnpublishedForUpdate(batchSize);
        if(batch.isEmpty()) return;

        for(OutboxEventEntity row : batch){
            try {
                CdrEvent event = objectMapper.readValue(row.getPayload(), CdrEvent.class);

                cdrEventPublisher.publish(event).get(5, TimeUnit.SECONDS);
                row.markPublished();
            } catch (JsonProcessingException e) {
                log.error("Error while converting CDR to CDR event", e);
                break;
            } catch (ExecutionException | InterruptedException | TimeoutException  e) {
                log.error("Error while publishing outbox event for id: {}, event-type: {}, error: {}",
                        row.getId(), row.getEventType(), e.getMessage());
                break;
            }
        }
    }
}
