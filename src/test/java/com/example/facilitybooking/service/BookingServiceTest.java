package com.example.facilitybooking.service;

import com.example.facilitybooking.dto.BookingResponse;
import com.example.facilitybooking.dto.CreateBookingRequest;
import com.example.facilitybooking.exception.BookingConflictException;
import com.example.facilitybooking.exception.BookingValidationException;
import com.example.facilitybooking.model.Booking;
import com.example.facilitybooking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private LocalDateTime futureStart;
    private LocalDateTime futureEnd;

    @BeforeEach
    void setUp() {
        futureStart = LocalDateTime.now().plusHours(1);
        futureEnd = futureStart.plusHours(1);
    }

    // ─── createBooking: happy path ────────────────────────────────────────────

    @Test
    void createBooking_validRequest_returnsBookingResponse() {
        CreateBookingRequest request = buildRequest("user1", "room-A", futureStart, futureEnd);

        Booking saved = Booking.builder()
                .id(1L)
                .userId("user1")
                .facilityId("room-A")
                .startTime(futureStart)
                .endTime(futureEnd)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any())).thenReturn(saved);

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo("user1");
        assertThat(response.getFacilityId()).isEqualTo("room-A");
        verify(bookingRepository).save(any(Booking.class));
    }

    // ─── createBooking: validation failures ───────────────────────────────────

    @Test
    void createBooking_endBeforeStart_throwsValidationException() {
        CreateBookingRequest request = buildRequest("user1", "room-A", futureEnd, futureStart);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void createBooking_startInPast_throwsValidationException() {
        CreateBookingRequest request = buildRequest("user1", "room-A",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusMinutes(30));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("cannot be made in the past");
    }

    @Test
    void createBooking_durationExceedsTwoHours_throwsValidationException() {
        CreateBookingRequest request = buildRequest("user1", "room-A",
                futureStart, futureStart.plusHours(3));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("Maximum booking duration is 2 hours");
    }

    @Test
    void createBooking_exactlyTwoHours_succeeds() {
        CreateBookingRequest request = buildRequest("user1", "room-A",
                futureStart, futureStart.plusHours(2));

        Booking saved = Booking.builder()
                .id(2L).userId("user1").facilityId("room-A")
                .startTime(futureStart).endTime(futureStart.plusHours(2))
                .createdAt(LocalDateTime.now()).build();

        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any())).thenReturn(saved);

        BookingResponse response = bookingService.createBooking(request);
        assertThat(response.getId()).isEqualTo(2L);
    }

    // ─── createBooking: conflict handling ─────────────────────────────────────

    @Test
    void createBooking_overlappingBookingExists_throwsConflictException() {
        CreateBookingRequest request = buildRequest("user1", "room-A", futureStart, futureEnd);

        Booking existing = Booking.builder()
                .id(99L).userId("user2").facilityId("room-A")
                .startTime(futureStart).endTime(futureEnd)
                .createdAt(LocalDateTime.now()).build();

        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("already booked");

        verify(bookingRepository, never()).save(any());
    }

    // ─── getBookingsByUser ─────────────────────────────────────────────────────

    @Test
    void getBookingsByUser_returnsMappedList() {
        Booking b1 = Booking.builder().id(1L).userId("user1").facilityId("room-A")
                .startTime(futureStart).endTime(futureEnd).createdAt(LocalDateTime.now()).build();
        Booking b2 = Booking.builder().id(2L).userId("user1").facilityId("room-B")
                .startTime(futureStart.plusDays(1)).endTime(futureEnd.plusDays(1))
                .createdAt(LocalDateTime.now()).build();

        when(bookingRepository.findByUserIdOrderByStartTimeAsc("user1")).thenReturn(List.of(b1, b2));

        List<BookingResponse> responses = bookingService.getBookingsByUser("user1");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getFacilityId()).isEqualTo("room-A");
        assertThat(responses.get(1).getFacilityId()).isEqualTo("room-B");
    }

    @Test
    void getBookingsByUser_noBookings_returnsEmptyList() {
        when(bookingRepository.findByUserIdOrderByStartTimeAsc("unknown")).thenReturn(List.of());

        List<BookingResponse> responses = bookingService.getBookingsByUser("unknown");

        assertThat(responses).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private CreateBookingRequest buildRequest(String userId, String facilityId,
                                               LocalDateTime start, LocalDateTime end) {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setUserId(userId);
        req.setFacilityId(facilityId);
        req.setStartTime(start);
        req.setEndTime(end);
        return req;
    }
}
