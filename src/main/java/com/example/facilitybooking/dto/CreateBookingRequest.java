package com.example.facilitybooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {

    @NotBlank(message = "Enter User ID")
    private String userId;

    @NotBlank(message = "Enter Facility ID")
    private String facilityId;

    @NotNull(message = "Please indicate start time")
    private LocalDateTime startTime;

    @NotNull(message = "Please indicate start time")
    private LocalDateTime endTime;
}
