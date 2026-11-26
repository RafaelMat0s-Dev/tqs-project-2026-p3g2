package pt.marazul.trip.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import pt.marazul.trip.application.ReservationExpiryService;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ReservationExpiryScheduler {
    private final ReservationExpiryService expiry;

    public ReservationExpiryScheduler(ReservationExpiryService expiry) {
        this.expiry = expiry;
    }

    @Scheduled(fixedDelayString = "${marazul.trip.expiry-interval:30s}")
    public void expireOverdueReservations() {
        expiry.expireOverdue();
    }
}
