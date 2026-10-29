package com.slotwise.service;

import com.slotwise.dto.BookingRequestDto;
import com.slotwise.dto.DashboardStatsDto;
import com.slotwise.entity.Booking;
import com.slotwise.entity.BookingStatus;
import com.slotwise.entity.Resource;
import com.slotwise.entity.User;
import com.slotwise.exception.BookingConflictException;
import com.slotwise.exception.InvalidBookingException;
import com.slotwise.exception.ResourceNotFoundException;
import com.slotwise.exception.UnauthorizedActionException;
import com.slotwise.repository.BookingRepository;
import com.slotwise.repository.ResourceRepository;
import com.slotwise.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public BookingService(BookingRepository bookingRepository,
                          ResourceRepository resourceRepository,
                          UserRepository userRepository,
                          MongoTemplate mongoTemplate) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public synchronized Booking createBooking(BookingRequestDto dto, User user) {
        // 1. Validate required fields
        if (dto.getResourceId() == null || dto.getResourceId().trim().isEmpty()) {
            throw new InvalidBookingException("Resource is required");
        }
        if (dto.getBookingDate() == null) {
            throw new InvalidBookingException("Booking date is required");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new InvalidBookingException("Start time and end time are required");
        }
        if (dto.getPurpose() == null || dto.getPurpose().trim().isEmpty()) {
            throw new InvalidBookingException("Purpose of booking is required");
        }

        // 2. Validate end time is after start time
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new InvalidBookingException("End time must be strictly after start time");
        }

        // 3. Validate booking is in the future
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        if (dto.getBookingDate().isBefore(today)) {
            throw new InvalidBookingException("Booking date cannot be in the past");
        }
        if (dto.getBookingDate().isEqual(today) && dto.getStartTime().isBefore(now)) {
            throw new InvalidBookingException("Booking start time cannot be in the past");
        }

        // 4. Validate resource exists and is active
        Resource resource = resourceRepository.findById(dto.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + dto.getResourceId()));

        if (!resource.isActive()) {
            throw new InvalidBookingException("Cannot book an inactive resource: " + resource.getName());
        }

        // 5. Backend Conflict Check
        // Existing booking conflicts if: existing.start < requested.end AND existing.end > requested.start
        // Adjacent slots (e.g. 10:00-11:00 and 11:00-12:00) do NOT conflict.
        List<Booking> conflicts = findConflicts(resource.getId(), dto.getBookingDate(), dto.getStartTime(), dto.getEndTime());

        if (!conflicts.isEmpty()) {
            Booking conflict = conflicts.get(0);
            throw new BookingConflictException(String.format(
                    "Time conflict: '%s' is already booked on %s from %s to %s for '%s'.",
                    resource.getName(),
                    conflict.getBookingDate(),
                    conflict.getStartTime(),
                    conflict.getEndTime(),
                    conflict.getPurpose()
            ));
        }

        // 6. Persist booking
        Booking booking = new Booking(
                user,
                resource,
                dto.getBookingDate(),
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getPurpose().trim()
        );

        return bookingRepository.save(booking);
    }

    public List<Booking> findConflicts(String resourceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Object refId = ObjectId.isValid(resourceId) ? new ObjectId(resourceId) : resourceId;

        Criteria resourceOr = new Criteria().orOperator(
                Criteria.where("resource.$id").is(refId),
                Criteria.where("resource.id").is(resourceId)
        );

        Criteria criteria = new Criteria().andOperator(
                resourceOr,
                Criteria.where("bookingDate").is(date),
                Criteria.where("status").is(BookingStatus.CONFIRMED),
                Criteria.where("startTime").lt(endTime),
                Criteria.where("endTime").gt(startTime)
        );

        return mongoTemplate.find(Query.query(criteria), Booking.class);
    }

    public Booking getById(String id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
    }

    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUserOrderByBookingDateDescStartTimeDesc(user);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingDateDescStartTimeDesc();
    }

    public Booking cancelBooking(String bookingId, User currentUser) {
        Booking booking = getById(bookingId);

        // Security check: Only the owner or an admin can cancel
        if (!currentUser.isAdmin() && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("This booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public DashboardStatsDto getDashboardStatistics() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        long activeResources = resourceRepository.countByActiveTrue();
        long totalBookings = bookingRepository.count();
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long totalUsers = userRepository.count();

        Criteria timeOr = new Criteria().orOperator(
                Criteria.where("bookingDate").gt(today),
                new Criteria().andOperator(
                        Criteria.where("bookingDate").is(today),
                        Criteria.where("endTime").gt(now)
                )
        );

        Criteria upcomingCriteria = new Criteria().andOperator(
                Criteria.where("status").is(BookingStatus.CONFIRMED),
                timeOr
        );

        long upcomingBookings = mongoTemplate.count(Query.query(upcomingCriteria), Booking.class);

        return new DashboardStatsDto(activeResources, totalBookings, upcomingBookings, cancelledBookings, totalUsers);
    }

    public List<Booking> getUpcomingBookingsForUser(User user) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Object userRefId = ObjectId.isValid(user.getId()) ? new ObjectId(user.getId()) : user.getId();

        Criteria userOr = new Criteria().orOperator(
                Criteria.where("user.$id").is(userRefId),
                Criteria.where("user.id").is(user.getId())
        );

        Criteria timeOr = new Criteria().orOperator(
                Criteria.where("bookingDate").gt(today),
                new Criteria().andOperator(
                        Criteria.where("bookingDate").is(today),
                        Criteria.where("endTime").gt(now)
                )
        );

        Criteria criteria = new Criteria().andOperator(
                userOr,
                Criteria.where("status").is(BookingStatus.CONFIRMED),
                timeOr
        );

        Query query = Query.query(criteria).with(Sort.by(Sort.Direction.ASC, "bookingDate", "startTime"));
        return mongoTemplate.find(query, Booking.class);
    }
}
