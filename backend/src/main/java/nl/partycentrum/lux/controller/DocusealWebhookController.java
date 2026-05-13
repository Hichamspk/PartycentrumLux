package nl.partycentrum.lux.controller;

import nl.partycentrum.lux.service.OfferteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/docuseal")
public class DocusealWebhookController {

    private final OfferteService offerteService;

    public DocusealWebhookController(OfferteService offerteService) {
        this.offerteService = offerteService;
    }

    @PostMapping
    public ResponseEntity<Void> completed(@RequestBody Map<String, Object> payload) {
        offerteService.handleDocusealWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
