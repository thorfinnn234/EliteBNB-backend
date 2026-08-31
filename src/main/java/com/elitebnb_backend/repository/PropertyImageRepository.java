package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyImageRepository
        extends JpaRepository<PropertyImage, Long> {

    List<PropertyImage> findByPropertyId(Long propertyId);

    long countByPropertyId(Long propertyId);
}