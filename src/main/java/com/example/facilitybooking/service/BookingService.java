package com.example.facilitybooking.service;

import com.example.facilitybooking.dto.BookingRequest;
import com.example.facilitybooking.model.Booking;
import com.example.facilitybooking.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {
        validate(request);

        boolean hasConflict = !bookingRepository
                .findOverlapping(request.getFacilityId(), request.getStartTime(), request.getEndTime())
                .isEmpty();

        if (hasConflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Facility already has a booking for that time slot.");
        }

        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setFacilityId(request.getFacilityId());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());

        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(String userId) {
        return bookingRepository.findByUserIdOrderByStartTimeAsc(userId);
    }

    private void validate(BookingRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime.");
        }
        if (!request.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking must be a future date.");
        }
        if (Duration.between(request.getStartTime(), request.getEndTime()).toHours() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum booking duration is 2 hours.");
        }
    }
}
