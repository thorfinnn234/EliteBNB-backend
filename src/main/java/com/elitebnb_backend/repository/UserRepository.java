package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    List<User> findByRole(Role role);

    long countByAccountStatus(AccountStatus accountStatus);

    @Query("""
            SELECT u
            FROM User u
            WHERE (:role IS NULL OR u.role = :role)
            AND (:status IS NULL OR u.accountStatus = :status)
            AND (
                :search IS NULL
                OR :search = ''
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            ORDER BY u.id DESC
            """)
    List<User> searchAdminUsers(
            @Param("search") String search,
            @Param("role") Role role,
            @Param("status") AccountStatus status
    );

    @Query("""
            SELECT FUNCTION('to_char', u.createdAt, 'YYYY-MM'), COUNT(u)
            FROM User u
            WHERE u.createdAt IS NOT NULL
            AND u.createdAt >= :start
            AND (:role IS NULL OR u.role = :role)
            GROUP BY FUNCTION('to_char', u.createdAt, 'YYYY-MM')
            ORDER BY FUNCTION('to_char', u.createdAt, 'YYYY-MM')
            """)
    List<Object[]> countUsersByMonth(
            @Param("start") LocalDateTime start,
            @Param("role") Role role
    );
}
