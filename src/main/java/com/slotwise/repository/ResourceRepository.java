package com.slotwise.repository;

import com.slotwise.entity.Resource;
import com.slotwise.entity.ResourceType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {

    List<Resource> findByActiveTrue();

    List<Resource> findByTypeAndActiveTrue(ResourceType type);

    long countByActiveTrue();
}
