package com.telcoedge.charging.web;


import com.telcoedge.charging.ChargingService;
import com.telcoedge.charging.OptimisticLockRetry;
import com.telcoedge.charging.dto.UsageHistoryDto;
import com.telcoedge.charging.event.CdrEvent;
import com.telcoedge.charging.event.CdrEventPublisher;
import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/charging")
public class ChargingController {

    private final ChargingService chargingService;
    private final CdrEventPublisher cdrPublisher;

    private Logger log = LoggerFactory.getLogger(ChargingController.class);

    public ChargingController(ChargingService chargingService,
                              CdrEventPublisher cdrPublisher) {
        this.chargingService = chargingService;
        this.cdrPublisher = cdrPublisher;
    }

    @RateLimiter(name = "chargingEndpoint", fallbackMethod = "rateLimitFallback")
    @PostMapping("/cdr")
    public ResponseEntity<ChargeResult> processCdr(@RequestBody CdrRequest request){
        try( var ignored1 = MDC.putCloseable("operatorId" , request.operatorId());
            var ignored2 = MDC.putCloseable("msisdn" , request.msisdn())) {

            ChargeResult result = chargingService.process(request.toCdr());

            cdrPublisher.publish(CdrEvent.fromCdr(request.toCdr()));

            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health(){
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/charge")
    public ResponseEntity<ChargeResult> charge(@RequestBody CdrRequest request){
        Cdr cdr = request.toCdr();
        ChargeResult result = OptimisticLockRetry.execute(()->
                chargingService.process(cdr));
        return ResponseEntity.ok(result);
    }

    private  ResponseEntity<ChargeResult> rateLimitFallback(@RequestBody CdrRequest request,
                                                            Exception ex){
        log.warn("Rate limit exceeded for CDR {}", request.eventId());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

}
