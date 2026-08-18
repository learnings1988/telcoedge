package com.telcoedge.charging.event;

import com.telcoedge.charging.config.KafkaTopicConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class CdrEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CdrEventConsumer.class);
    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();
    private final MeterRegistry meterRegistry;

    public CdrEventConsumer(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics= KafkaTopicConfig.CDR_EVENTS_TOPIC)
    public void onCdrEvent(CdrEvent event){
        if(event == null || event.eventId() == null){
            log.warn("skipping CDR event with null payload or eventId");
            return;
        }
        if(!isFirstDelivery(event.eventId())){
            count("duplicate" , event);
            log.debug("Duplicate CDR event ignored: eventId={}", event.eventId());
            return;
        }

        count("processed", event);
        log.info("Consumed CDR: eventId={}, msisdn={}, usageType={}, quantity={}",
                event.eventId(), event.msisdn(),event.usageType(),event.quantity());

    }

    private boolean isFirstDelivery(UUID eventId){
        return processedEventIds.add(eventId);
    }

    private void count(String outcome, CdrEvent event){
        Counter.builder("cdr.consumed")
                .description("cdr charged event consumed from cdr-events")
                .tag("outcome", outcome)
                .tag("usage_type" , event.usageType().name())
                .register(meterRegistry)
                .increment();
    }

    public boolean hasProcessed(UUID eventId){
        return processedEventIds.contains(eventId);
    }

    public int processedCount(){
        return processedEventIds.size();
    }
}
