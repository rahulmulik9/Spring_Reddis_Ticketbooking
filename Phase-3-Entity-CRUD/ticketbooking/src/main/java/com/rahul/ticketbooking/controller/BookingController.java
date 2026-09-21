package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.BookingResponse;
import com.rahul.ticketbooking.dto.HoldResponse;
import com.rahul.ticketbooking.security.CurrentUser;
import com.rahul.ticketbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUser currentUser;

    @PostMapping("/events/{eventId}/seats/{seatId}/hold")
    public ResponseEntity<HoldResponse> hold(@PathVariable Long eventId, @PathVariable Long seatId) {
        return ResponseEntity.ok(bookingService.hold(eventId, seatId, currentUser.getId()));
    }

    @PostMapping("/events/{eventId}/seats/{seatId}/confirm")
    public ResponseEntity<BookingResponse> confirm(@PathVariable Long eventId, @PathVariable Long seatId) {
        return ResponseEntity.ok(bookingService.confirm(eventId, seatId, currentUser.getId()));
    }

    // simple endpoint: always 204, no body, so @ResponseStatus is enough
    @DeleteMapping("/events/{eventId}/seats/{seatId}/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long eventId, @PathVariable Long seatId) {
        bookingService.release(eventId, seatId, currentUser.getId());
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancel(id, currentUser.getId()));
    }

    @GetMapping("/bookings/me")
    public ResponseEntity<List<BookingResponse>> myBookings() {
        return ResponseEntity.ok(bookingService.myBookings(currentUser.getId()));
    }
}