package com.elitebnb_backend.service;

import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.HostVerificationStatus;
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
            "Host verification is required before using Host business features";

    private final UserRepository userRepository;
    private final HostVerificationRepository hostVerificationRepository;

    /**
     * Resolves the authenticated principal to a database user, then enforces the
     * verified-host business rule from one place. Services use the returned
     * entity when they need the current Host for repository queries.
     */
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

    /**
     * Loads a Host by email and applies the same business-access rule used for
     * JWT-authenticated requests. This keeps controller/service call sites from
     * reimplementing role, account-status, and verification checks.
     */
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

    /**
     * Enforces the product definition of normal Host business access:
     * role HOST, active account, and a VERIFIED HostVerification row.
     */
    public void requireVerifiedBusinessAccess(
            User host
    ) {

        if (!hasBusinessAccess(host)) {
            throw new AccessDeniedException(
                    BUSINESS_ACCESS_DENIED_MESSAGE
            );
        }
    }

    /**
     * Answers whether the supplied user currently has Host business access.
     * Admin approval takes effect immediately because HostVerification remains
     * the source of truth; no duplicate "verified" flag is stored on User.
     */
    public boolean hasBusinessAccess(
            User host
    ) {

        if (host == null
                || host.getRole() != Role.HOST
                || host.getAccountStatus() != AccountStatus.ACTIVE) {
            return false;
        }

        return hostVerificationRepository
                .findByHost(host)
                .map(verification ->
                        verification.getStatus()
                                == HostVerificationStatus.VERIFIED
                )
                .orElse(false);
    }
}
