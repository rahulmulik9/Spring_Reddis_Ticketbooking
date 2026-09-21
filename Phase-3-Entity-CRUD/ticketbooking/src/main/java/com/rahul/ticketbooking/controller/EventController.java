package com.rahul.ticketbooking.controller;

import com.rahul.ticketbooking.dto.EventResponse;
import com.rahul.ticketbooking.dto.SeatResponse;
import com.rahul.ticketbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> list() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.findById(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatResponse>> seats(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getSeats(id));
    }
}