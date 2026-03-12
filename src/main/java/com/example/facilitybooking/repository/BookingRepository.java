package com.example.facilitybooking.repository;

import com.example.facilitybooking.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // PESSIMISTIC_WRITE = SELECT FOR UPDATE.
    // If two requests check for conflicts at the same time, only one gets the lock.
    // The other waits, then re-checks — and sees the first booking already saved.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.facilityId = :facilityId AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findOverlapping(@Param("facilityId") String facilityId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    List<Booking> findByUserIdOrderByStartTimeAsc(String userId);
}
