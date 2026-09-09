package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // All bookings made by a particular guest
    List<Booking> findByGuest(User guest);

    // All bookings for a particular property
    List<Booking> findByProperty(Property property);

    // Host reservation list:
    // Get bookings for all properties owned by a host
    List<Booking> findByPropertyHost(User host);

    // Get bookings by status
    List<Booking> findByStatus(BookingStatus status);

    // Get a host's bookings by status
    List<Booking> findByPropertyHostAndStatus(
            User host,
            BookingStatus status
    );

    // Check if dates overlap an existing active booking
    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.property.id = :propertyId
            AND b.status IN :statuses
            AND b.checkIn < :checkOut
            AND b.checkOut > :checkIn
            """)
    boolean existsOverlappingBooking(
            @Param("propertyId") Long propertyId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("statuses") List<BookingStatus> statuses
    );

    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0)
        FROM Booking b
        WHERE b.property.host = :host
        AND b.status = :status
        """)
    Double sumHostRevenueByStatus(
            @Param("host") User host,
            @Param("status") BookingStatus status
    );

    long countByPropertyHost(User host);

    long countByPropertyHostAndStatus(
            User host,
            BookingStatus status
    );

    long countByStatus(BookingStatus status);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE (
                :search IS NULL
                OR :search = ''
                OR (:bookingIdSearch IS NOT NULL AND b.id = :bookingIdSearch)
                OR LOWER(b.property.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(b.property.location) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(b.property.host.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(b.property.host.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(b.property.host.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(b.guest.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(b.guest.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(b.guest.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:status IS NULL OR b.status = :status)
            AND (:propertyId IS NULL OR b.property.id = :propertyId)
            AND (:guestId IS NULL OR b.guest.id = :guestId)
            AND (:hostId IS NULL OR b.property.host.id = :hostId)
            AND (:from IS NULL OR b.checkOut >= :from)
            AND (:to IS NULL OR b.checkIn <= :to)
            ORDER BY b.id DESC
            """)
    List<Booking> searchAdminBookings(
            @Param("search") String search,
            @Param("bookingIdSearch") Long bookingIdSearch,
            @Param("status") BookingStatus status,
            @Param("propertyId") Long propertyId,
            @Param("guestId") Long guestId,
            @Param("hostId") Long hostId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            SELECT FUNCTION('to_char', b.createdAt, 'YYYY-MM'), COUNT(b)
            FROM Booking b
            WHERE b.createdAt >= :start
            GROUP BY FUNCTION('to_char', b.createdAt, 'YYYY-MM')
            ORDER BY FUNCTION('to_char', b.createdAt, 'YYYY-MM')
            """)
    List<Object[]> countBookingsByMonth(
            @Param("start") LocalDateTime start
    );
}
