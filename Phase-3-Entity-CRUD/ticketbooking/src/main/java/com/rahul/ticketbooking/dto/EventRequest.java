package com.rahul.ticketbooking.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EventRequest {
    private String name;
    private String venue;
    private LocalDateTime date;
}
