package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.EventRequest;
import com.rahul.ticketbooking.dto.EventResponse;
import com.rahul.ticketbooking.dto.SeatResponse;
import com.rahul.ticketbooking.entity.Event;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.SeatStatus;
import com.rahul.ticketbooking.repository.EventRepository;
import com.rahul.ticketbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    // 3.5 reads
    public List<EventResponse> findAll() {
        return eventRepository.findAll().stream().map(EventResponse::from).toList();
    }

    public EventResponse findById(Long id) {
        return EventResponse.from(getEvent(id));
    }

    // 3.3 admin CRUD
    @Transactional
    public EventResponse create(EventRequest req) {
        Event event = new Event();
        event.setName(req.getName());
        event.setVenue(req.getVenue());
        event.setDate(req.getDate());
        event.setSeatCount(100);
        Event saved = eventRepository.save(event);
        generateSeats(saved.getId());   // 3.4
        return EventResponse.from(saved);
    }

    @Transactional
    public EventResponse update(Long id, EventRequest req) {
        Event event = getEvent(id);
        event.setName(req.getName());
        event.setVenue(req.getVenue());
        event.setDate(req.getDate());
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long id) {
        getEvent(id);
        seatRepository.deleteByEventId(id);
        eventRepository.deleteById(id);
    }

    // 3.4 seats A1..J10
    private void generateSeats(Long eventId) {
        List<Seat> seats = new ArrayList<>();
        for (char row = 'A'; row <= 'J'; row++) {
            for (int n = 1; n <= 10; n++) {
                seats.add(new Seat(null, eventId, row + "" + n, SeatStatus.AVAILABLE, null, null));
            }
        }
        seatRepository.saveAll(seats);
    }

    // 3.6 seat map with lazy hold-expiry check
    @Transactional
    public List<SeatResponse> getSeats(Long eventId) {
        getEvent(eventId);
        List<Seat> seats = seatRepository.findByEventIdOrderByIdAsc(eventId);
        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.HELD
                    && seat.getHoldExpiresAt() != null
                    && seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldBy(null);
                seat.setHoldExpiresAt(null);
            }
        }
        return seats.stream().map(SeatResponse::from).toList();
    }

    private Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }
}