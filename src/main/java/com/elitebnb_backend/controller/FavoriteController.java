package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.FavoriteResponse;
import com.elitebnb_backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getMyFavorites(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                favoriteService.getMyFavorites(
                        authentication.getName()
                )
        );
    }

    @PostMapping("/{propertyId}")
    public ResponseEntity<FavoriteResponse> addFavorite(
            @PathVariable Long propertyId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                favoriteService.addFavorite(
                        propertyId,
                        authentication.getName()
                )
        );
    }

    @DeleteMapping("/{propertyId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long propertyId,
            Authentication authentication
    ) {
        favoriteService.removeFavorite(
                propertyId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{propertyId}/status")
    public ResponseEntity<Map<String, Boolean>> isFavorite(
            @PathVariable Long propertyId,
            Authentication authentication
    ) {
        boolean saved = favoriteService.isFavorite(
                propertyId,
                authentication.getName()
        );

        return ResponseEntity.ok(
                Map.of("saved", saved)
        );
    }
}