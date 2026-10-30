package com.slotwise.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Document(collection = "bookings")
@CompoundIndex(name = "idx_booking_conflict", def = "{'resource.$id': 1, 'bookingDate': 1, 'status': 1, 'startTime': 1, 'endTime': 1}")
public class Booking {

    @Id
    private String id;

    @NotNull
    @DBRef
    private User user;

    @NotNull
    @DBRef
    private Resource resource;

    private String userId;
    private String userName;
    private String userEmail;

    private String resourceId;
    private String resourceName;
    private ResourceType resourceType;
    private String resourceLocation;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    @NotNull
    private BookingStatus status = BookingStatus.CONFIRMED;

    private LocalDateTime createdAt;

    public Booking() {
        this.createdAt = LocalDateTime.now();
        this.status = BookingStatus.CONFIRMED;
    }

    public Booking(User user, Resource resource, LocalDate bookingDate, LocalTime startTime, LocalTime endTime, String purpose) {
        this.setUser(user);
        this.setResource(resource);
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        if (this.user == null && (this.userName != null || this.userEmail != null || this.userId != null)) {
            User fallback = new User();
            fallback.setId(this.userId != null ? this.userId : "N/A");
            fallback.setFullName(this.userName != null ? this.userName : "User");
            fallback.setEmail(this.userEmail != null ? this.userEmail : "");
            return fallback;
        }
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            if (user.getId() != null) this.userId = user.getId();
            if (user.getFullName() != null) this.userName = user.getFullName();
            if (user.getEmail() != null) this.userEmail = user.getEmail().trim().toLowerCase();
        }
    }

    public Resource getResource() {
        if (this.resource == null && (this.resourceName != null || this.resourceId != null)) {
            Resource fallback = new Resource();
            fallback.setId(this.resourceId != null ? this.resourceId : "N/A");
            fallback.setName(this.resourceName != null ? this.resourceName : "Campus Resource");
            fallback.setType(this.resourceType != null ? this.resourceType : ResourceType.ROOM);
            fallback.setLocation(this.resourceLocation != null ? this.resourceLocation : "Campus");
            return fallback;
        }
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
        if (resource != null) {
            if (resource.getId() != null) this.resourceId = resource.getId();
            if (resource.getName() != null) this.resourceName = resource.getName();
            if (resource.getType() != null) this.resourceType = resource.getType();
            if (resource.getLocation() != null) this.resourceLocation = resource.getLocation();
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail != null ? userEmail.trim().toLowerCase() : null;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceLocation() {
        return resourceLocation;
    }

    public void setResourceLocation(String resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isConfirmed() {
        return BookingStatus.CONFIRMED.equals(this.status);
    }

    public boolean isCancelled() {
        return BookingStatus.CANCELLED.equals(this.status);
    }
}
