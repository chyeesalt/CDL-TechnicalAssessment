package com.example.facilitybooking.controller;

import com.example.facilitybooking.dto.BookingResponse;
import com.example.facilitybooking.dto.CreateBookingRequest;
import com.example.facilitybooking.exception.BookingConflictException;
import com.example.facilitybooking.exception.BookingValidationException;
import com.example.facilitybooking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private final LocalDateTime futureStart = LocalDateTime.now().plusHours(1);
    private final LocalDateTime futureEnd = futureStart.plusHours(1);

    // POST /api/bookings

    @Test
    void createBooking_validRequest_returns201() throws Exception {
        CreateBookingRequest request = buildRequest("user1", "room-A", futureStart, futureEnd);

        BookingResponse response = BookingResponse.builder()
                .id(1L).userId("user1").facilityId("room-A")
                .startTime(futureStart).endTime(futureEnd)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingService.createBooking(any())).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value("user1"))
                .andExpect(jsonPath("$.facilityId").value("room-A"));
    }

    @Test
    void createBooking_missingUserId_returns400() throws Exception {
        CreateBookingRequest request = buildRequest(null, "room-A", futureStart, futureEnd);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void createBooking_missingStartTime_returns400() throws Exception {
        CreateBookingRequest request = buildRequest("user1", "room-A", null, futureEnd);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_pastStartTime_returns400() throws Exception {
        CreateBookingRequest request = buildRequest("user1", "room-A", futureStart, futureEnd);

        when(bookingService.createBooking(any()))
                .thenThrow(new BookingValidationException("Bookings cannot be made in the past."));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Booking"));
    }

    @Test
    void createBooking_conflict_returns409() throws Exception {
        CreateBookingRequest request = buildRequest("user1", "room-A", futureStart, futureEnd);

        when(bookingService.createBooking(any()))
                .thenThrow(new BookingConflictException("Facility 'room-A' is already booked."));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Booking Conflict"));
    }

    // GET /api/bookings

    @Test
    void getBookings_validUserId_returnsListWith200() throws Exception {
        BookingResponse b1 = BookingResponse.builder()
                .id(1L).userId("user1").facilityId("room-A")
                .startTime(futureStart).endTime(futureEnd)
                .createdAt(LocalDateTime.now()).build();

        when(bookingService.getBookingsByUser(eq("user1"))).thenReturn(List.of(b1));

        mockMvc.perform(get("/api/bookings").param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].facilityId").value("room-A"));
    }

    @Test
    void getBookings_noBookings_returnsEmptyList() throws Exception {
        when(bookingService.getBookingsByUser(eq("nobody"))).thenReturn(List.of());

        mockMvc.perform(get("/api/bookings").param("userId", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getBookings_missingUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isBadRequest());
    }

    // helpers

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
