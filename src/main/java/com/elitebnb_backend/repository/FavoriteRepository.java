package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Favorite;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserOrderByCreatedAtDesc(User user);

    Optional<Favorite> findByUserAndProperty(User user, Property property);

    boolean existsByUserAndProperty(User user, Property property);

    void deleteByUserAndProperty(User user, Property property);
}