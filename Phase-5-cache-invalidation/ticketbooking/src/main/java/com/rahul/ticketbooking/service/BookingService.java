package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingResponse;
import com.rahul.ticketbooking.dto.HoldResponse;
import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.entity.BookingStatus;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.SeatStatus;
import com.rahul.ticketbooking.repository.BookingRepository;
import com.rahul.ticketbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final CacheManager cacheManager;

    @CacheEvict(cacheNames = "seats", key = "#eventId")
    @Transactional
    public HoldResponse hold(Long eventId, Long seatId, Long userId) {
        Seat seat = getSeat(eventId, seatId);

        if (isHoldExpired(seat)) {
            clearHold(seat);
        }

        // CHECK
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not available");
        }

        // race window: another request can pass the same check here

        // THEN SAVE
        seat.setStatus(SeatStatus.HELD);
        seat.setHeldBy(userId);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        seatRepository.save(seat);

        return HoldResponse.from(seat);
    }

    @CacheEvict(cacheNames = "seats", key = "#eventId")
    @Transactional
    public BookingResponse confirm(Long eventId, Long seatId, Long userId) {
        Seat seat = getSeat(eventId, seatId);

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not held");
        }
        if (!userId.equals(seat.getHeldBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seat is held by someone else");
        }
        if (isHoldExpired(seat)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hold has expired");
        }

        seat.setStatus(SeatStatus.BOOKED);
        seat.setHeldBy(null);
        seat.setHoldExpiresAt(null);
        seatRepository.save(seat);

        Booking booking = new Booking(null, userId, seat.getId(), LocalDateTime.now(), BookingStatus.CONFIRMED);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @CacheEvict(cacheNames = "seats", key = "#eventId")
    @Transactional
    public void release(Long eventId, Long seatId, Long userId) {
        Seat seat = getSeat(eventId, seatId);

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not held");
        }
        if (!userId.equals(seat.getHeldBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not hold this seat");
        }

        clearHold(seat);
        seatRepository.save(seat);
    }

    // 3.10 cancel own booking
    @Transactional
    public BookingResponse cancel(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Seat seat = seatRepository.findById(booking.getSeatId()).orElseThrow();
        clearHold(seat);
        seatRepository.save(seat);
        cacheManager.getCache("seats").evict(seat.getEventId());
        return BookingResponse.from(booking);
    }

    // 3.10 my bookings
    public List<BookingResponse> myBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(BookingResponse::from).toList();
    }

    // ---- helpers ----

    private Seat getSeat(Long eventId, Long seatId) {
        return seatRepository.findByIdAndEventId(seatId, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seat not found"));
    }

    private boolean isHoldExpired(Seat seat) {
        return seat.getStatus() == SeatStatus.HELD
                && seat.getHoldExpiresAt() != null
                && seat.getHoldExpiresAt().isBefore(LocalDateTime.now());
    }

    // sets the seat back to AVAILABLE
    private void clearHold(Seat seat) {
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setHeldBy(null);
        seat.setHoldExpiresAt(null);
    }
}