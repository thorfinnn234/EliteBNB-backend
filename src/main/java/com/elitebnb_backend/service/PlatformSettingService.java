package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.PlatformSettingResponse;
import com.elitebnb_backend.dto.UpdatePlatformSettingRequest;
import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditTargetType;
import com.elitebnb_backend.entity.PlatformSetting;
import com.elitebnb_backend.entity.PlatformSettingKey;
import com.elitebnb_backend.repository.PlatformSettingRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformSettingService {

    private final PlatformSettingRepository platformSettingRepository;
    private final AuditLogService auditLogService;

    public List<PlatformSettingResponse> getSettings() {

        ensureDefaultSettings();

        return platformSettingRepository
                .findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    public PlatformSettingResponse getSetting(
            PlatformSettingKey key
    ) {

        ensureDefaultSettings();

        return map(
                getSettingEntity(key)
        );
    }

    public boolean isRefundsEnabled() {

        ensureDefaultSettings();

        return Boolean.parseBoolean(
                getSettingEntity(
                        PlatformSettingKey.REFUNDS_ENABLED
                )
                        .getSettingValue()
        );
    }

    public PlatformSettingResponse updateSetting(
            PlatformSettingKey key,
            UpdatePlatformSettingRequest request,
            String adminEmail
    ) {

        if (request.getValue() == null
                || request.getValue().trim().isEmpty()) {

            throw new RuntimeException(
                    "Setting value is required"
            );
        }

        validateValue(key, request.getValue());

        ensureDefaultSettings();

        PlatformSetting setting =
                getSettingEntity(key);

        setting.setSettingValue(
                request.getValue().trim()
        );

        if (request.getDescription() != null) {
            setting.setDescription(
                    normalize(request.getDescription())
            );
        }

        PlatformSetting saved =
                platformSettingRepository.save(setting);

        auditLogService.record(
                adminEmail,
                AuditActionType.PLATFORM_SETTING_UPDATED,
                AuditTargetType.PLATFORM_SETTING,
                saved.getId(),
                "Updated platform setting "
                        + saved.getSettingKey().name()
        );

        return map(saved);
    }

    private void ensureDefaultSettings() {

        createDefaultIfMissing(
                PlatformSettingKey.SERVICE_FEE_PERCENTAGE,
                "5.00",
                "Percentage service fee charged by the platform"
        );

        createDefaultIfMissing(
                PlatformSettingKey.SUPPORT_EMAIL,
                "support@elitebnb.com",
                "Public support contact email"
        );

        createDefaultIfMissing(
                PlatformSettingKey.MAINTENANCE_MODE,
                "false",
                "Whether maintenance mode is enabled"
        );

        createDefaultIfMissing(
                PlatformSettingKey.REFUNDS_ENABLED,
                "true",
                "Whether refund requests can be submitted"
        );
    }

    private void createDefaultIfMissing(
            PlatformSettingKey key,
            String value,
            String description
    ) {

        if (platformSettingRepository
                .findBySettingKey(key)
                .isPresent()) {
            return;
        }

        PlatformSetting setting =
                PlatformSetting.builder()
                        .settingKey(key)
                        .settingValue(value)
                        .description(description)
                        .build();

        platformSettingRepository.save(setting);
    }

    private PlatformSetting getSettingEntity(
            PlatformSettingKey key
    ) {

        return platformSettingRepository
                .findBySettingKey(key)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Platform setting not found"
                        )
                );
    }

    private void validateValue(
            PlatformSettingKey key,
            String value
    ) {

        String trimmed =
                value.trim();

        switch (key) {

            case SERVICE_FEE_PERCENTAGE -> {
                BigDecimal percentage;

                try {
                    percentage =
                            new BigDecimal(trimmed);
                } catch (NumberFormatException exception) {
                    throw new RuntimeException(
                            "Service fee percentage must be numeric"
                    );
                }

                if (percentage.compareTo(BigDecimal.ZERO) < 0
                        || percentage.compareTo(
                        BigDecimal.valueOf(100)
                ) > 0) {

                    throw new RuntimeException(
                            "Service fee percentage must be between 0 and 100"
                    );
                }
            }

            case SUPPORT_EMAIL -> {
                if (!trimmed.contains("@")) {
                    throw new RuntimeException(
                            "Support email must be a valid email address"
                    );
                }
            }

            case MAINTENANCE_MODE, REFUNDS_ENABLED -> {
                if (!"true".equalsIgnoreCase(trimmed)
                        && !"false".equalsIgnoreCase(trimmed)) {

                    throw new RuntimeException(
                            key.name()
                                    + " must be true or false"
                    );
                }
            }
        }
    }

    private String normalize(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private PlatformSettingResponse map(
            PlatformSetting setting
    ) {

        return new PlatformSettingResponse(
                setting.getId(),
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDescription(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }
}
