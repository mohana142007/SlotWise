package com.slotwise.service;

import com.mongodb.DBRef;
import com.slotwise.dto.BookingRequestDto;
import com.slotwise.dto.DashboardStatsDto;
import com.slotwise.entity.Booking;
import com.slotwise.entity.BookingStatus;
import com.slotwise.entity.Resource;
import com.slotwise.entity.ResourceType;
import com.slotwise.entity.Role;
import com.slotwise.entity.User;
import com.slotwise.exception.BookingConflictException;
import com.slotwise.exception.InvalidBookingException;
import com.slotwise.exception.ResourceNotFoundException;
import com.slotwise.exception.UnauthorizedActionException;
import com.slotwise.repository.BookingRepository;
import com.slotwise.repository.ResourceRepository;
import com.slotwise.repository.UserRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
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
        booking.setUserId(user.getId());
        booking.setUserName(user.getFullName());
        booking.setUserEmail(user.getEmail() != null ? user.getEmail().trim().toLowerCase() : null);
        booking.setResourceId(resource.getId());
        booking.setResourceName(resource.getName());
        booking.setResourceType(resource.getType());
        booking.setResourceLocation(resource.getLocation());

        return bookingRepository.save(booking);
    }

    public List<Booking> findConflicts(String resourceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Object refId = (resourceId != null && ObjectId.isValid(resourceId)) ? new ObjectId(resourceId) : resourceId;

        Criteria resourceOr = new Criteria().orOperator(
                Criteria.where("resource.$id").is(refId),
                Criteria.where("resource.$id").is(resourceId),
                Criteria.where("resource.id").is(resourceId),
                Criteria.where("resourceId").is(resourceId)
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
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
        return ensureReferences(booking);
    }

    public List<Booking> getBookingsByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }

        Object userRefId = (user.getId() != null && ObjectId.isValid(user.getId()))
                ? new ObjectId(user.getId())
                : user.getId();

        Criteria userCriteria = new Criteria().orOperator(
                Criteria.where("user.$id").is(userRefId),
                Criteria.where("user.$id").is(user.getId()),
                Criteria.where("user.id").is(user.getId()),
                Criteria.where("userId").is(user.getId()),
                Criteria.where("userEmail").is(user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "")
        );

        Query query = Query.query(userCriteria)
                .with(Sort.by(Sort.Direction.DESC, "bookingDate", "startTime"));

        List<Booking> bookings = mongoTemplate.find(query, Booking.class);
        bookings.forEach(this::ensureReferences);
        return bookings;
    }

    public List<Booking> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAllByOrderByBookingDateDescStartTimeDesc();
        bookings.forEach(this::ensureReferences);
        return bookings;
    }

    public Booking cancelBooking(String bookingId, User currentUser) {
        Booking booking = getById(bookingId);

        boolean isOwner = false;
        if (booking.getUser() != null && booking.getUser().getId() != null && currentUser.getId() != null) {
            isOwner = booking.getUser().getId().equals(currentUser.getId());
        }
        if (!isOwner && booking.getUserId() != null && currentUser.getId() != null) {
            isOwner = booking.getUserId().equals(currentUser.getId());
        }
        if (!isOwner && booking.getUserEmail() != null && currentUser.getEmail() != null) {
            isOwner = booking.getUserEmail().equalsIgnoreCase(currentUser.getEmail().trim());
        }

        // Security check: Only the owner or an admin can cancel
        if (!currentUser.isAdmin() && !isOwner) {
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
        if (user == null) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Object userRefId = (user.getId() != null && ObjectId.isValid(user.getId()))
                ? new ObjectId(user.getId())
                : user.getId();

        Criteria userOr = new Criteria().orOperator(
                Criteria.where("user.$id").is(userRefId),
                Criteria.where("user.$id").is(user.getId()),
                Criteria.where("user.id").is(user.getId()),
                Criteria.where("userId").is(user.getId()),
                Criteria.where("userEmail").is(user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "")
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
        List<Booking> bookings = mongoTemplate.find(query, Booking.class);
        bookings.forEach(this::ensureReferences);
        return bookings;
    }

    public Booking ensureReferences(Booking booking) {
        if (booking == null) {
            return null;
        }

        // 1. Resolve User
        if (booking.getUser() == null || booking.getUser().getFullName() == null) {
            User resolvedUser = null;
            if (booking.getUserId() != null) {
                resolvedUser = userRepository.findById(booking.getUserId()).orElse(null);
            }
            if (resolvedUser == null && booking.getUserEmail() != null) {
                resolvedUser = userRepository.findByEmail(booking.getUserEmail()).orElse(null);
            }
            if (resolvedUser == null && booking.getId() != null) {
                resolvedUser = extractUserFromRawDoc(booking.getId());
            }

            if (resolvedUser != null) {
                booking.setUser(resolvedUser);
            } else {
                User placeholder = new User();
                placeholder.setId(booking.getUserId() != null ? booking.getUserId() : "N/A");
                placeholder.setFullName(booking.getUserName() != null ? booking.getUserName() : "Registered User");
                placeholder.setEmail(booking.getUserEmail() != null ? booking.getUserEmail() : "N/A");
                placeholder.setRole(Role.ROLE_STUDENT);
                booking.setUser(placeholder);
            }
        }

        // 2. Resolve Resource
        if (booking.getResource() == null || booking.getResource().getName() == null) {
            Resource resolvedResource = null;
            if (booking.getResourceId() != null) {
                resolvedResource = resourceRepository.findById(booking.getResourceId()).orElse(null);
            }
            if (resolvedResource == null && booking.getId() != null) {
                resolvedResource = extractResourceFromRawDoc(booking.getId());
            }

            if (resolvedResource != null) {
                booking.setResource(resolvedResource);
            } else {
                Resource placeholder = new Resource();
                placeholder.setId(booking.getResourceId() != null ? booking.getResourceId() : "N/A");
                placeholder.setName(booking.getResourceName() != null ? booking.getResourceName() : "Campus Resource");
                placeholder.setType(booking.getResourceType() != null ? booking.getResourceType() : ResourceType.ROOM);
                placeholder.setLocation(booking.getResourceLocation() != null ? booking.getResourceLocation() : "Campus");
                booking.setResource(placeholder);
            }
        }

        return booking;
    }

    private User extractUserFromRawDoc(String bookingId) {
        try {
            Object idObj = ObjectId.isValid(bookingId) ? new ObjectId(bookingId) : bookingId;
            Document doc = mongoTemplate.getCollection("bookings")
                    .find(new Document("_id", idObj))
                    .first();
            if (doc != null && doc.get("user") instanceof DBRef ref) {
                Object uid = ref.getId();
                String uidStr = uid.toString();
                return userRepository.findById(uidStr).orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Resource extractResourceFromRawDoc(String bookingId) {
        try {
            Object idObj = ObjectId.isValid(bookingId) ? new ObjectId(bookingId) : bookingId;
            Document doc = mongoTemplate.getCollection("bookings")
                    .find(new Document("_id", idObj))
                    .first();
            if (doc != null && doc.get("resource") instanceof DBRef ref) {
                Object rid = ref.getId();
                String ridStr = rid.toString();
                return resourceRepository.findById(ridStr).orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
