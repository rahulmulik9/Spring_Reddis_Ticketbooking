package com.rahul.ticketbooking.dto;

import com.rahul.ticketbooking.entity.Seat;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class HoldResponse {
    private Long seatId;
    private String label;
    private LocalDateTime holdExpiresAt;

    public static HoldResponse from(Seat s) {
        return new HoldResponse(s.getId(), s.getLabel(), s.getHoldExpiresAt());
    }
}