package com.elitebnb_backend.service;

import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.HostVerificationRepository;
import com.elitebnb_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HostAccessService {

    private static final String BUSINESS_ACCESS_DENIED_MESSAGE =
            "Host business access is unavailable";

    private final UserRepository userRepository;
    private final HostVerificationRepository hostVerificationRepository;

    public User getVerifiedHost(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new RuntimeException(
                    "Authentication required"
            );
        }

        return requireVerifiedBusinessAccess(
                authentication.getName()
        );
    }

    public User requireVerifiedBusinessAccess(
            String hostEmail
    ) {

        User host =
                userRepository
                        .findByEmail(hostEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Host not found"
                                )
                        );

        requireVerifiedBusinessAccess(host);

        return host;
    }

    public void requireVerifiedBusinessAccess(
            User host
    ) {

        if (!hasBusinessAccess(host)) {
            throw new AccessDeniedException(
                    BUSINESS_ACCESS_DENIED_MESSAGE
            );
        }
    }

    public boolean hasBusinessAccess(
            User host
    ) {

        return host != null
                && host.getRole() == Role.HOST
                && host.getAccountStatus() == AccountStatus.ACTIVE;
    }
}