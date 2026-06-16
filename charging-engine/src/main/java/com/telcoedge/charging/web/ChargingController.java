package com.telcoedge.charging.web;


import com.telcoedge.charging.ChargingService;
import com.telcoedge.charging.OptimisticLockRetry;
import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/charging")
public class ChargingController {

    private final ChargingService chargingService;

    public ChargingController(ChargingService chargingService) {
        this.chargingService = chargingService;
    }

    @PostMapping("/cdr")
    public ResponseEntity<ChargeResult> processCdr(@RequestBody CdrRequest request){
        ChargeResult result = chargingService.process(request.toCdr());
        return ResponseEntity.ok(result);
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
}
