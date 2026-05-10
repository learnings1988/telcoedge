package com.telcoedge.subscriber.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
public class HealthCheckController {
    private static final Logger log = LoggerFactory.getLogger(HealthCheckController.class);

    @GetMapping("/api/v1/ping")
    public Map<String, Object> ping(){
        Thread current = Thread.currentThread();
        log.info("ping received on thread: {} (virtual={})" , current, current.isVirtual());

        return Map.of(
                "service","subscriber-api",
                "status", "ok",
                "thread", current.toString(),
                "virtual", current.isVirtual()
        );
    }
}
