package com.telcoedge.charging;


import com.telcoedge.charging.dto.UsageHistoryDto;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usage")
public class UsageHistoryController {

    private final UsageHistoryService usageHistoryService;

    public UsageHistoryController(UsageHistoryService usageHistoryService) {
        this.usageHistoryService = usageHistoryService;
    }

    @GetMapping("/{operatorId}/{msisdn}")
    public ResponseEntity<Page<UsageHistoryDto>> getUsageHistory(
            @PathVariable String operatorId,
            @PathVariable String msisdn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {

        if(size>100){
            size = 100;
        }

        Page<UsageHistoryDto> result;
        if(type!=null && !type.isBlank()){
            result = usageHistoryService.getHistoryByType( operatorId, msisdn,
                    type.toUpperCase(), page, size);
        }else {
            result = usageHistoryService.getHistory(operatorId, msisdn, page, size);
        }
        return ResponseEntity.ok(result);
    }
}
