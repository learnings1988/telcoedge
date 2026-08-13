package com.telcoedge.charging.event;

import com.telcoedge.charging.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CdrEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger("CdrEventPublisher.class");
    private static final String TOPIC = KafkaTopicConfig.CDR_EVENTS_TOPIC;
    private final KafkaTemplate<String , CdrEvent> kafkaTemplate;

    public CdrEventPublisher(KafkaTemplate<String, CdrEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, CdrEvent>> publish(CdrEvent event){
        logger.debug("Publishing CDR event: eventid={}, msisdn={}", event.eventId(), event.msisdn());

        return kafkaTemplate.send(TOPIC, event.msisdn(), event)
                .whenComplete((result, ex)->{
                    if(ex!=null){
                        logger.error("failed to publish CDR event: eventid={}, msisdn={}, error = {}",
                                event.eventId(), event.msisdn(), ex.getMessage());
                    }else{
                        logger.debug("CDR event published: eventId = {}, partition={}, offset={}",
                                event.eventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                        });
    }
}
