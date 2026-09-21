package com.rahul.ticketbooking.dto;

import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.entity.BookingStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long seatId;
    private BookingStatus status;
    private LocalDateTime createdAt;

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getSeatId(), b.getStatus(), b.getCreatedAt());
    }
}