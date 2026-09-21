package com.rahul.ticketbooking.dto;

import com.rahul.ticketbooking.entity.Event;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EventResponse {
    private Long id;
    private String name;
    private String venue;
    private LocalDateTime date;
    private int seatCount;

    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getName(), e.getVenue(), e.getDate(), e.getSeatCount());
    }
}
