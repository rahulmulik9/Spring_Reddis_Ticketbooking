package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.BookingResponse;
import com.rahul.ticketbooking.dto.HoldResponse;
import com.rahul.ticketbooking.security.CurrentUser;
import com.rahul.ticketbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUser currentUser;

    @PostMapping("/events/{eventId}/seats/{seatId}/hold")
    public HoldResponse hold(@PathVariable Long eventId, @PathVariable Long seatId) {
        return bookingService.hold(eventId, seatId, currentUser.getId());
    }

    @PostMapping("/events/{eventId}/seats/{seatId}/confirm")
    public BookingResponse confirm(@PathVariable Long eventId, @PathVariable Long seatId) {
        return bookingService.confirm(eventId, seatId, currentUser.getId());
    }

    @DeleteMapping("/events/{eventId}/seats/{seatId}/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long eventId, @PathVariable Long seatId) {
        bookingService.release(eventId, seatId, currentUser.getId());
    }

    @PostMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id, currentUser.getId());
    }

    @GetMapping("/bookings/me")
    public List<BookingResponse> myBookings() {
        return bookingService.myBookings(currentUser.getId());
    }
}