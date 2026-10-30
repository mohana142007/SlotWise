package com.slotwise.service;

import com.slotwise.dto.BookingRequestDto;
import com.slotwise.entity.*;
import com.slotwise.exception.BookingConflictException;
import com.slotwise.exception.InvalidBookingException;
import com.slotwise.exception.UnauthorizedActionException;
import com.slotwise.repository.BookingRepository;
import com.slotwise.repository.ResourceRepository;
import com.slotwise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private BookingService bookingService;

    private User student1;
    private User student2;
    private User admin;
    private Resource seminarRoom;
    private Resource projector;

    @BeforeEach
    void setUp() {
        student1 = new User("Alice Brown", "alice@slotwise.edu", "hashedPwd", Role.ROLE_STUDENT);
        student1.setId("usr1");

        student2 = new User("Bob White", "bob@slotwise.edu", "hashedPwd", Role.ROLE_STUDENT);
        student2.setId("usr2");

        admin = new User("Admin Staff", "admin@slotwise.edu", "hashedPwd", Role.ROLE_ADMIN);
        admin.setId("adm99");

        seminarRoom = new Resource("Seminar Room A", ResourceType.ROOM, "Description", "Building 1", 50, true);
        seminarRoom.setId("res10");

        projector = new Resource("Projector 1", ResourceType.EQUIPMENT, "Description", "AV Room", null, true);
        projector.setId("res20");
    }

    @Test
    @DisplayName("Successful booking creation when slot is free")
    void testSuccessfulBooking() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(seminarRoom.getId());
        dto.setBookingDate(tomorrow);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setPurpose("Project Meeting");

        when(resourceRepository.findById("res10")).thenReturn(Optional.of(seminarRoom));
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking created = bookingService.createBooking(dto, student1);

        assertNotNull(created);
        assertEquals(BookingStatus.CONFIRMED, created.getStatus());
        assertEquals("Project Meeting", created.getPurpose());
        assertEquals(student1, created.getUser());
        assertEquals(seminarRoom, created.getResource());
    }

    @Test
    @DisplayName("Rejection of overlapping bookings for the same resource")
    void testRejectionOfOverlappingBookingsSameResource() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 30);
        LocalTime end = LocalTime.of(11, 30);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(seminarRoom.getId());
        dto.setBookingDate(tomorrow);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setPurpose("Overlapping attempt");

        Booking existing = new Booking(student2, seminarRoom, tomorrow, LocalTime.of(10, 0), LocalTime.of(11, 0), "Existing Booking");

        when(resourceRepository.findById("res10")).thenReturn(Optional.of(seminarRoom));
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(List.of(existing));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(dto, student1));
    }

    @Test
    @DisplayName("Allowing adjacent time slots (e.g., 10:00-11:00 and 11:00-12:00)")
    void testAllowAdjacentTimeSlots() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(11, 0);
        LocalTime end = LocalTime.of(12, 0);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(seminarRoom.getId());
        dto.setBookingDate(tomorrow);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setPurpose("Adjacent Slot Booking");

        when(resourceRepository.findById("res10")).thenReturn(Optional.of(seminarRoom));
        // Database query returns empty list because adjacent slot does not overlap
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(dto, student1);
        assertNotNull(booking);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    @DisplayName("Allowing simultaneous time slots for different resources")
    void testAllowSimultaneousTimeSlotsDifferentResources() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(projector.getId());
        dto.setBookingDate(tomorrow);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setPurpose("Presentation Projector");

        when(resourceRepository.findById("res20")).thenReturn(Optional.of(projector));
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(dto, student1);
        assertNotNull(booking);
        assertEquals(projector.getId(), booking.getResource().getId());
    }

    @Test
    @DisplayName("Cancelled bookings no longer block availability")
    void testCancelledBookingNoLongerBlocksAvailability() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(seminarRoom.getId());
        dto.setBookingDate(tomorrow);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setPurpose("New reservation after cancellation");

        when(resourceRepository.findById("res10")).thenReturn(Optional.of(seminarRoom));
        // Cancelled bookings do not match status: CONFIRMED
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(dto, student1);
        assertNotNull(booking);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    @DisplayName("Rejection of past booking date")
    void testRejectionOfPastDate() {
        LocalDate pastDate = LocalDate.now().minusDays(1);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(seminarRoom.getId());
        dto.setBookingDate(pastDate);
        dto.setStartTime(LocalTime.of(10, 0));
        dto.setEndTime(LocalTime.of(11, 0));
        dto.setPurpose("Past date attempt");

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(dto, student1));
    }

    @Test
    @DisplayName("Rejection of invalid times (end time before start time)")
    void testRejectionOfInvalidTimes() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(seminarRoom.getId());
        dto.setBookingDate(tomorrow);
        dto.setStartTime(LocalTime.of(14, 0));
        dto.setEndTime(LocalTime.of(13, 0));
        dto.setPurpose("Invalid hours");

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(dto, student1));
    }

    @Test
    @DisplayName("Rejection of booking for inactive resource")
    void testRejectionOfInactiveResource() {
        seminarRoom.setActive(false);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(seminarRoom.getId());
        dto.setBookingDate(tomorrow);
        dto.setStartTime(LocalTime.of(10, 0));
        dto.setEndTime(LocalTime.of(11, 0));
        dto.setPurpose("Inactive resource attempt");

        when(resourceRepository.findById("res10")).thenReturn(Optional.of(seminarRoom));

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(dto, student1));
    }

    @Test
    @DisplayName("Users cannot cancel another user's booking")
    void testUserCannotCancelAnotherUserBooking() {
        Booking booking = new Booking(student1, seminarRoom, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0), "Alice's booking");
        booking.setId("bk100");

        when(bookingRepository.findById("bk100")).thenReturn(Optional.of(booking));

        // student2 tries to cancel student1's booking
        assertThrows(UnauthorizedActionException.class, () -> bookingService.cancelBooking("bk100", student2));
    }

    @Test
    @DisplayName("Administrator can cancel any user's booking")
    void testAdminCanCancelAnyUserBooking() {
        Booking booking = new Booking(student1, seminarRoom, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0), "Alice's booking");
        booking.setId("bk100");

        when(bookingRepository.findById("bk100")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking cancelled = bookingService.cancelBooking("bk100", admin);
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("getUpcomingBookingsForUser constructs valid Criteria without multiple $or exception")
    void testUpcomingBookingsForUserConstructsValidCriteria() {
        LocalDate today = LocalDate.now();
        Booking todayBooking = new Booking(student1, seminarRoom, today, LocalTime.now().plusHours(1), LocalTime.now().plusHours(2), "Today session");
        Booking futureBooking = new Booking(student1, seminarRoom, today.plusDays(2), LocalTime.of(14, 0), LocalTime.of(15, 0), "Future session");

        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(List.of(todayBooking, futureBooking));

        // Must execute cleanly without throwing InvalidMongoDbApiUsageException: Criteria already contains '$or'
        List<Booking> results = assertDoesNotThrow(() -> bookingService.getUpcomingBookingsForUser(student1));

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Today session", results.get(0).getPurpose());
        assertEquals("Future session", results.get(1).getPurpose());
    }

    @Test
    @DisplayName("getUpcomingBookingsForUser properly isolates criteria to specified user")
    void testUpcomingBookingsForUserIsolatesUser() {
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> bookingService.getUpcomingBookingsForUser(student2));

        verify(mongoTemplate, times(1)).find(argThat(query -> {
            org.bson.Document doc = query.getQueryObject();
            // Verify that the query contains $and and does not throw BSON encoding exceptions
            return doc.containsKey("$and");
        }), eq(Booking.class));
    }

    @Test
    @DisplayName("Verify Query BSON structure has both user and time OR expressions nested under $and")
    void testQueryStructureContainsSeparateOrConditionsUnderAnd() {
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(Collections.emptyList());

        bookingService.getUpcomingBookingsForUser(student1);

        verify(mongoTemplate).find(argThat(query -> {
            org.bson.Document doc = query.getQueryObject();
            assertTrue(doc.containsKey("$and"), "Query must contain $and at the top level");
            @SuppressWarnings("unchecked")
            List<org.bson.Document> andList = (List<org.bson.Document>) doc.get("$and");
            assertEquals(3, andList.size(), "Should have 3 and-clauses: userOr, status, timeOr");

            // Clause 1: userOr has $or
            assertTrue(andList.get(0).containsKey("$or"));
            // Clause 2: status is CONFIRMED
            assertEquals(BookingStatus.CONFIRMED, andList.get(1).get("status"));
            // Clause 3: timeOr has $or
            assertTrue(andList.get(2).containsKey("$or"));
            return true;
        }), eq(Booking.class));
    }

    @Test
    @DisplayName("getBookingsByUser queries mongoTemplate with user criteria and resolves references safely")
    void testGetBookingsByUserQueriesAndEnsuresReferences() {
        Booking booking = new Booking(student1, seminarRoom, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0), "Lab Session");
        booking.setId("bk201");

        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(List.of(booking));

        List<Booking> results = bookingService.getBookingsByUser(student1);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Lab Session", results.get(0).getPurpose());
        assertEquals(student1.getFullName(), results.get(0).getUser().getFullName());
        assertEquals(seminarRoom.getName(), results.get(0).getResource().getName());
    }

    @Test
    @DisplayName("getAllBookings provides safe fallback placeholders when user or resource reference is null")
    void testGetAllBookingsHandlesNullReferencesSafely() {
        Booking orphanBooking = new Booking();
        orphanBooking.setId("orphan1");
        orphanBooking.setBookingDate(LocalDate.now().plusDays(1));
        orphanBooking.setStartTime(LocalTime.of(10, 0));
        orphanBooking.setEndTime(LocalTime.of(11, 0));
        orphanBooking.setPurpose("Legacy Booking");
        // user and resource are null

        when(bookingRepository.findAllByOrderByBookingDateDescStartTimeDesc()).thenReturn(List.of(orphanBooking));

        List<Booking> results = bookingService.getAllBookings();

        assertNotNull(results);
        assertEquals(1, results.size());
        Booking b = results.get(0);
        assertNotNull(b.getUser(), "User fallback must not be null");
        assertNotNull(b.getUser().getFullName(), "User full name must not be null");
        assertNotNull(b.getResource(), "Resource fallback must not be null");
        assertNotNull(b.getResource().getName(), "Resource name must not be null");
        assertNotNull(b.getResource().getType(), "Resource type must not be null");
    }
}
