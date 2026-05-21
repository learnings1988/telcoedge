package com.telcoedge.subscriber.web;

import com.telcoedge.domain.Subscriber;
import com.telcoedge.subscriber.service.SubscriberService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operators/{operatorId}/subscribers")
public class SubscriberController {

    private final SubscriberService service;

    public SubscriberController(SubscriberService service) {
        this.service = service;
    }

    public record createSubscriberRequest(String msisdn, String name){}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subscriber create(@PathVariable String operatorId,
                            @RequestBody createSubscriberRequest req){
        return service.create(operatorId, req.msisdn, req.name);
    }

    @GetMapping("/{msisdn}")
    public Subscriber get(@PathVariable String operatorId,
                          @PathVariable String msisdn){
        return service.findByMsisdn( operatorId, msisdn);
    }
}
