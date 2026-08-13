package com.telcoedge.charging.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    public static final String CDR_EVENTS_TOPIC = "cdr-events";

    @Bean
    public NewTopic cdrEventsTopic(){
        return TopicBuilder.name(CDR_EVENTS_TOPIC)
                .partitions(12)
                .replicas(1)
                .build();
    }
}
