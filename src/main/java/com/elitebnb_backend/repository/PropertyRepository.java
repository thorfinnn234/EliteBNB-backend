package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository
        extends JpaRepository<Property, Long>,
        JpaSpecificationExecutor<Property> {

    List<Property> findByHost(User host);

    List<Property> findByStatus(PropertyStatus status);

    @Query("""
            SELECT p
            FROM Property p
            WHERE p.status = com.elitebnb_backend.entity.PropertyStatus.ACTIVE
            AND (
                p.approvalStatus = com.elitebnb_backend.entity.PropertyApprovalStatus.APPROVED
                OR p.approvalStatus IS NULL
            )
            ORDER BY p.id DESC
            """)
    List<Property> findPublicVisibleProperties();

    List<Property> findByPropertyType(PropertyType propertyType);

    List<Property> findByLocationContainingIgnoreCase(
            String location
    );

    List<Property> findByLocationContainingIgnoreCaseAndStatus(
            String location,
            PropertyStatus status
    );

    long countByHost(User host);

    long countByHostAndStatus(
            User host,
            PropertyStatus status
    );

    long countByStatus(
            PropertyStatus status
    );

    long countByApprovalStatus(
            PropertyApprovalStatus approvalStatus
    );

    @Query("""
            SELECT p
            FROM Property p
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.location) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.host.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.host.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.host.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:status IS NULL OR p.status = :status)
            AND (:approvalStatus IS NULL OR p.approvalStatus = :approvalStatus)
            AND (:propertyType IS NULL OR p.propertyType = :propertyType)
            ORDER BY p.id DESC
            """)
    List<Property> searchAdminProperties(
            @Param("search") String search,
            @Param("status") PropertyStatus status,
            @Param("approvalStatus") PropertyApprovalStatus approvalStatus,
            @Param("propertyType") PropertyType propertyType
    );

    List<Property> findByApprovalStatusOrderByCreatedAtDesc(
            PropertyApprovalStatus approvalStatus
    );
}
