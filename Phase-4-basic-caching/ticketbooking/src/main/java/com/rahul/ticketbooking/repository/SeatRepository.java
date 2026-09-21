package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventIdOrderByIdAsc(Long eventId);

    void deleteByEventId(Long eventId);

    Optional<Seat> findByIdAndEventId(Long id, Long eventId);
}