package com.example.facilitybooking;

import com.example.facilitybooking.dto.BookingRequest;
import com.example.facilitybooking.model.Booking;
import com.example.facilitybooking.repository.BookingRepository;
import com.example.facilitybooking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;

    @BeforeEach
    void cleanUp() {
        bookingRepository.deleteAll();
    }

    // ── Service tests ────────────────────────────────────────────────────────

    @Test
    void createBooking_valid_savesToDatabase() {
        Booking booking = bookingService.createBooking(request("user1", "room-A",
                tomorrow(10), tomorrow(11)));

        assertThat(booking.getId()).isNotNull();
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void createBooking_pastTime_throwsBadRequest() {
        assertThatThrownBy(() -> bookingService.createBooking(
                request("user1", "room-A", yesterday(), yesterday().plusHours(1))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("past");
    }

    @Test
    void createBooking_endBeforeStart_throwsBadRequest() {
        assertThatThrownBy(() -> bookingService.createBooking(
                request("user1", "room-A", tomorrow(11), tomorrow(10))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("endTime");
    }

    @Test
    void createBooking_exceedsTwoHours_throwsBadRequest() {
        assertThatThrownBy(() -> bookingService.createBooking(
                request("user1", "room-A", tomorrow(10), tomorrow(13))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("2 hours");
    }

    @Test
    void createBooking_conflict_throwsConflict() {
        bookingService.createBooking(request("user1", "room-A", tomorrow(10), tomorrow(11)));

        assertThatThrownBy(() -> bookingService.createBooking(
                request("user2", "room-A", tomorrow(10), tomorrow(11))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    void createBooking_sameFacilityNonOverlapping_succeeds() {
        bookingService.createBooking(request("user1", "room-A", tomorrow(9), tomorrow(10)));
        bookingService.createBooking(request("user2", "room-A", tomorrow(10), tomorrow(11)));

        assertThat(bookingRepository.count()).isEqualTo(2);
    }

    @Test
    void getBookingsForUser_returnsOnlyThatUsersBookings() {
        bookingService.createBooking(request("user1", "room-A", tomorrow(9), tomorrow(10)));
        bookingService.createBooking(request("user2", "room-B", tomorrow(10), tomorrow(11)));

        assertThat(bookingService.getBookingsForUser("user1")).hasSize(1);
        assertThat(bookingService.getBookingsForUser("user2")).hasSize(1);
    }

    // ── Controller / HTTP tests ───────────────────────────────────────────────

    @Test
    void POST_validBooking_returns201() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("user1", "room-A", tomorrow(10), tomorrow(11))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.facilityId").value("room-A"));
    }

    @Test
    void POST_missingUserId_returns400() throws Exception {
        BookingRequest req = request(null, "room-A", tomorrow(10), tomorrow(11));
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_conflictingBooking_returns409() throws Exception {
        bookingService.createBooking(request("user1", "room-A", tomorrow(10), tomorrow(11)));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("user2", "room-A", tomorrow(10), tomorrow(11))))
                .andExpect(status().isConflict());
    }

    @Test
    void GET_returnsBookingsForUser() throws Exception {
        bookingService.createBooking(request("user1", "room-A", tomorrow(9), tomorrow(10)));

        mockMvc.perform(get("/api/bookings").param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void GET_noBookings_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/bookings").param("userId", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BookingRequest request(String userId, String facilityId,
                                   LocalDateTime start, LocalDateTime end) {
        BookingRequest r = new BookingRequest();
        r.setUserId(userId);
        r.setFacilityId(facilityId);
        r.setStartTime(start);
        r.setEndTime(end);
        return r;
    }

    private String json(String userId, String facilityId,
                        LocalDateTime start, LocalDateTime end) throws Exception {
        return objectMapper.writeValueAsString(request(userId, facilityId, start, end));
    }

    private LocalDateTime tomorrow(int hour) {
        return LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    private LocalDateTime yesterday() {
        return LocalDateTime.now().minusDays(1);
    }
}
