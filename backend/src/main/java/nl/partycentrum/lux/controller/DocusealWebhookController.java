package nl.partycentrum.lux.controller;

import nl.partycentrum.lux.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/docuseal")
public class DocusealWebhookController {

    private final ContractService contractService;

    public DocusealWebhookController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    public ResponseEntity<Void> completed(@RequestBody Map<String, Object> payload) {
        contractService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
