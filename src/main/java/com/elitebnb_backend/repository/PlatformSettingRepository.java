package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.PlatformSetting;
import com.elitebnb_backend.entity.PlatformSettingKey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformSettingRepository
        extends JpaRepository<PlatformSetting, Long> {

    Optional<PlatformSetting> findBySettingKey(
            PlatformSettingKey settingKey
    );
}
