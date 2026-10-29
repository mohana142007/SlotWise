package com.slotwise.repository;

import com.slotwise.entity.Booking;
import com.slotwise.entity.BookingStatus;
import com.slotwise.entity.Resource;
import com.slotwise.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    @Query("{ 'resource.$id': ?0, 'bookingDate': ?1, 'status': 'CONFIRMED', 'startTime': { $lt: ?3 }, 'endTime': { $gt: ?2 } }")
    List<Booking> findConflictingBookings(Object resourceId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime);

    List<Booking> findByUserOrderByBookingDateDescStartTimeDesc(User user);

    List<Booking> findAllByOrderByBookingDateDescStartTimeDesc();

    long countByStatus(BookingStatus status);

    List<Booking> findByResourceAndBookingDateAndStatusOrderByStartTimeAsc(Resource resource, LocalDate bookingDate, BookingStatus status);
}
