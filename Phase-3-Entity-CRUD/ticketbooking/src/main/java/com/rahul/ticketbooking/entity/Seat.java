package com.rahul.ticketbooking.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private String label;

    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    private Long heldBy;
    private LocalDateTime holdExpiresAt;

    // no @Version on purpose
}