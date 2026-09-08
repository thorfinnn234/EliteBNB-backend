package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.PlatformSettingResponse;
import com.elitebnb_backend.dto.UpdatePlatformSettingRequest;
import com.elitebnb_backend.entity.PlatformSettingKey;
import com.elitebnb_backend.service.PlatformSettingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminPlatformSettingController {

    private final PlatformSettingService platformSettingService;

    @GetMapping
    public ResponseEntity<List<PlatformSettingResponse>> getSettings() {

        return ResponseEntity.ok(
                platformSettingService.getSettings()
        );
    }

    @GetMapping("/{key}")
    public ResponseEntity<PlatformSettingResponse> getSetting(
            @PathVariable PlatformSettingKey key
    ) {

        return ResponseEntity.ok(
                platformSettingService.getSetting(key)
        );
    }

    @PatchMapping("/{key}")
    public ResponseEntity<PlatformSettingResponse> updateSetting(
            @PathVariable PlatformSettingKey key,
            @RequestBody UpdatePlatformSettingRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                platformSettingService.updateSetting(
                        key,
                        request,
                        authentication.getName()
                )
        );
    }
}
