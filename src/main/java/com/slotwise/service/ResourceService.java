package com.slotwise.service;

import com.slotwise.dto.AvailabilityCheckDto;
import com.slotwise.dto.ResourceDto;
import com.slotwise.entity.Booking;
import com.slotwise.entity.BookingStatus;
import com.slotwise.entity.Resource;
import com.slotwise.exception.ResourceNotFoundException;
import com.slotwise.repository.BookingRepository;
import com.slotwise.repository.ResourceRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;
    private final MongoTemplate mongoTemplate;

    public ResourceService(ResourceRepository resourceRepository,
                           BookingRepository bookingRepository,
                           MongoTemplate mongoTemplate) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<Resource> findAll() {
        return resourceRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<Resource> findAllActive() {
        return resourceRepository.findByActiveTrue();
    }

    public Resource getById(String id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
    }

    public List<Resource> search(AvailabilityCheckDto filter, boolean onlyActive) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (onlyActive) {
            criteriaList.add(Criteria.where("active").is(true));
        }

        if (filter.getType() != null) {
            criteriaList.add(Criteria.where("type").is(filter.getType()));
        }

        if (filter.getLocation() != null && !filter.getLocation().trim().isEmpty()) {
            criteriaList.add(Criteria.where("location").regex(filter.getLocation().trim(), "i"));
        }

        if (filter.getQuery() != null && !filter.getQuery().trim().isEmpty()) {
            String q = filter.getQuery().trim();
            Criteria searchOr = new Criteria().orOperator(
                    Criteria.where("name").regex(q, "i"),
                    Criteria.where("description").regex(q, "i"),
                    Criteria.where("location").regex(q, "i")
            );
            criteriaList.add(searchOr);
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        query.with(Sort.by(Sort.Direction.ASC, "name"));

        List<Resource> candidates = mongoTemplate.find(query, Resource.class);

        // If time slot is provided, filter available candidates
        if (filter.hasTimeSlot()) {
            return candidates.stream()
                    .filter(resource -> isAvailable(resource.getId(), filter.getDate(), filter.getStartTime(), filter.getEndTime()))
                    .toList();
        }
        return candidates;
    }

    public boolean isAvailable(String resourceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Resource resource = getById(resourceId);
        if (!resource.isActive()) {
            return false;
        }

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

        List<Booking> conflicts = mongoTemplate.find(Query.query(criteria), Booking.class);
        return conflicts.isEmpty();
    }

    public List<Booking> getConfirmedBookingsForDate(String resourceId, LocalDate date) {
        Resource resource = getById(resourceId);
        return bookingRepository.findByResourceAndBookingDateAndStatusOrderByStartTimeAsc(
                resource, date, BookingStatus.CONFIRMED
        );
    }

    public Resource createResource(ResourceDto dto) {
        Resource resource = new Resource(
                dto.getName().trim(),
                dto.getType(),
                dto.getDescription() != null ? dto.getDescription().trim() : null,
                dto.getLocation().trim(),
                dto.getCapacity(),
                dto.isActive()
        );
        return resourceRepository.save(resource);
    }

    public Resource updateResource(String id, ResourceDto dto) {
        Resource resource = getById(id);
        resource.setName(dto.getName().trim());
        resource.setType(dto.getType());
        resource.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        resource.setLocation(dto.getLocation().trim());
        resource.setCapacity(dto.getCapacity());
        resource.setActive(dto.isActive());
        return resourceRepository.save(resource);
    }

    public boolean toggleActiveStatus(String id) {
        Resource resource = getById(id);
        resource.setActive(!resource.isActive());
        resourceRepository.save(resource);
        return resource.isActive();
    }

    public long countActiveResources() {
        return resourceRepository.countByActiveTrue();
    }

    public long countTotalResources() {
        return resourceRepository.count();
    }
}
