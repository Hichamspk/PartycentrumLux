package nl.partycentrum.lux.controller;

import nl.partycentrum.lux.dto.scheduled.ScheduledJobRunResponse;
import nl.partycentrum.lux.service.ScheduledJobService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/scheduled-jobs")
@PreAuthorize("hasRole('OWNER')")
public class ScheduledJobController {

    private final ScheduledJobService scheduledJobService;

    public ScheduledJobController(ScheduledJobService scheduledJobService) {
        this.scheduledJobService = scheduledJobService;
    }

    @PostMapping("/run-daily")
    public ScheduledJobRunResponse runDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        var runDate = date == null ? LocalDate.now() : date;
        scheduledJobService.sendPaymentReminders(runDate);
        scheduledJobService.sendEventReminders(runDate);
        scheduledJobService.autoFinishFullyPaidBookings(runDate);
        scheduledJobService.sendReviewRequests(runDate);
        return new ScheduledJobRunResponse(true, runDate, "Dagelijkse jobs handmatig uitgevoerd.");
    }
}
