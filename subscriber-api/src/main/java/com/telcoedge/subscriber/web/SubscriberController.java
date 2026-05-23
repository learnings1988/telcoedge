package com.telcoedge.subscriber.web;

import com.telcoedge.domain.Subscriber;
import com.telcoedge.subscriber.service.SubscriberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operators/{operatorId}/subscribers")
public class SubscriberController {

    private final SubscriberService service;

    public SubscriberController(SubscriberService service) {
        this.service = service;
    }

    public record createSubscriberRequest(
            @NotBlank(message = "msisdn is required")
            @Size(min = 10, max = 15, message = "msisdn must be 10-15 digits")
            String msisdn,
            @NotBlank(message = "name is required")
            @Size(max = 200, message = "name must not exceed 200 characters")
            String name){}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subscriber create(@PathVariable String operatorId,
                            @Valid @RequestBody createSubscriberRequest req){
        return service.create(operatorId, req.msisdn, req.name);
    }

    @GetMapping("/{msisdn}")
    public Subscriber get(@PathVariable String operatorId,
                          @PathVariable String msisdn){
        return service.findByMsisdn( operatorId, msisdn);
    }
}
