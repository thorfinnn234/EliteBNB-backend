package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PropertyRepository
        extends JpaRepository<Property, Long>,
        JpaSpecificationExecutor<Property> {

    List<Property> findByHost(User host);

    List<Property> findByStatus(PropertyStatus status);

    List<Property> findByPropertyType(PropertyType propertyType);

    List<Property> findByLocationContainingIgnoreCase(
            String location
    );

    List<Property>
    findByLocationContainingIgnoreCaseAndStatus(
            String location,
            PropertyStatus status
    );

    long countByHost(User host);

    long countByHostAndStatus(
            User host,
            PropertyStatus status
    );

    long countByStatus(PropertyStatus status);
}