package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminUserResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditTargetType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    // =========================
    // GET ALL / SEARCH / FILTER
    // =========================

    public List<AdminUserResponse> getUsers(
            String search,
            Role role,
            AccountStatus status
    ) {

        return userRepository
                .searchAdminUsers(
                        search,
                        role,
                        status
                )
                .stream()
                .map(this::map)
                .toList();
    }

    // =========================
    // GET ONE USER
    // =========================

    public AdminUserResponse getUser(
            Long userId
    ) {

        User user = getUserEntity(userId);

        return map(user);
    }

    // =========================
    // UPDATE ACCOUNT STATUS
    // =========================

    public AdminUserResponse updateStatus(
            Long userId,
            AccountStatus status,
            String adminEmail
    ) {

        if (status == null) {
            throw new RuntimeException(
                    "Account status is required"
            );
        }

        User targetUser =
                getUserEntity(userId);

        User admin =
                userRepository.findByEmail(adminEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Admin account not found"
                                )
                        );

        // Admin cannot suspend themselves
        if (targetUser.getId()
                .equals(admin.getId())) {

            throw new RuntimeException(
                    "You cannot suspend your own admin account"
            );
        }

        // For now, protect other admins too
        if (targetUser.getRole()
                == Role.ADMIN) {

            throw new RuntimeException(
                    "Admin accounts cannot be suspended here"
            );
        }

        targetUser.setAccountStatus(status);

        User saved =
                userRepository.save(targetUser);

        auditLogService.record(
                adminEmail,
                AuditActionType.USER_STATUS_UPDATED,
                AuditTargetType.USER,
                saved.getId(),
                "Updated user "
                        + saved.getEmail()
                        + " account status to "
                        + status.name()
        );

        return map(saved);
    }

    // =========================
    // HELPERS
    // =========================

    private User getUserEntity(
            Long userId
    ) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }

    private AdminUserResponse map(
            User user
    ) {

        return new AdminUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getLocation(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getAccountStatus(),
                user.isEmailVerified(),
                user.isHostOnboardingCompleted()
        );
    }
}
