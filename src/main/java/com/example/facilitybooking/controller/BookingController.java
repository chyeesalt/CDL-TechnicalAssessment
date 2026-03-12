package com.example.facilitybooking.controller;

import com.example.facilitybooking.dto.BookingRequest;
import com.example.facilitybooking.model.Booking;
import com.example.facilitybooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking createBooking(@Valid @RequestBody BookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping
    public List<Booking> getBookings(@RequestParam String userId) {
        return bookingService.getBookingsForUser(userId);
    }
}
