package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.PropertyAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PropertyAvailabilityRepository
        extends JpaRepository<PropertyAvailability, Long> {

    List<PropertyAvailability> findByPropertyId(Long propertyId);

    @Query("""
            SELECT COUNT(a) > 0
            FROM PropertyAvailability a
            WHERE a.property.id = :propertyId
            AND a.startDate < :checkOut
            AND a.endDate > :checkIn
            """)
    boolean existsOverlappingBlock(
            @Param("propertyId") Long propertyId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}