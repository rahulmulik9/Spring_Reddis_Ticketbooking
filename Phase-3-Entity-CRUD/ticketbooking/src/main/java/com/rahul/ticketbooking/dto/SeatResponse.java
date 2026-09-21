package com.rahul.ticketbooking.dto;

import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.SeatStatus;
import lombok.*;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String label;
    private SeatStatus status;

    public static SeatResponse from(Seat s) {
        return new SeatResponse(s.getId(), s.getLabel(), s.getStatus());
    }
}