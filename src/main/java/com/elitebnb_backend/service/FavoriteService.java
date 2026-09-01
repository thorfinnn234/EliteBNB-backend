package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.FavoriteResponse;
import com.elitebnb_backend.entity.Favorite;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyImage;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.FavoriteRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public FavoriteResponse addFavorite(
            Long propertyId,
            String userEmail
    ) {
        User user = getUser(userEmail);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() ->
                        new RuntimeException("Property not found")
                );

        if (favoriteRepository.existsByUserAndProperty(user, property)) {
            throw new RuntimeException(
                    "Property is already in your wishlist"
            );
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .property(property)
                .build();

        return mapToResponse(
                favoriteRepository.save(favorite)
        );
    }

    public List<FavoriteResponse> getMyFavorites(
            String userEmail
    ) {
        User user = getUser(userEmail);

        return favoriteRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public boolean isFavorite(
            Long propertyId,
            String userEmail
    ) {
        User user = getUser(userEmail);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() ->
                        new RuntimeException("Property not found")
                );

        return favoriteRepository
                .existsByUserAndProperty(user, property);
    }

    @Transactional
    public void removeFavorite(
            Long propertyId,
            String userEmail
    ) {
        User user = getUser(userEmail);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() ->
                        new RuntimeException("Property not found")
                );

        Favorite favorite = favoriteRepository
                .findByUserAndProperty(user, property)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property is not in your wishlist"
                        )
                );

        favoriteRepository.delete(favorite);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );
    }

    private FavoriteResponse mapToResponse(
            Favorite favorite
    ) {
        Property property = favorite.getProperty();

        String coverImageUrl = property.getImages()
                .stream()
                .filter(PropertyImage::isCoverImage)
                .map(PropertyImage::getImageUrl)
                .findFirst()
                .orElseGet(() ->
                        property.getImages()
                                .stream()
                                .map(PropertyImage::getImageUrl)
                                .findFirst()
                                .orElse(null)
                );

        return new FavoriteResponse(
                favorite.getId(),
                property.getId(),
                property.getTitle(),
                property.getLocation(),
                property.getPricePerNight(),
                property.getPropertyType().name(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                coverImageUrl,
                favorite.getCreatedAt()
        );
    }
}