package com.elitebnb_backend.config;

import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates the first development ADMIN account only when explicitly enabled.
 *
 * This runner intentionally reads credentials from environment-backed Spring
 * properties so real Admin passwords are never stored in source control. It is
 * designed for local/bootstrap use only: public registration remains limited to
 * USER and HOST accounts.
 */
@Component
public class DevelopmentAdminBootstrap implements ApplicationRunner {

    static final String ENABLED_PROPERTY =
            "ELITEBNB_ADMIN_BOOTSTRAP_ENABLED";
    static final String EMAIL_PROPERTY =
            "ELITEBNB_ADMIN_EMAIL";
    static final String PASSWORD_PROPERTY =
            "ELITEBNB_ADMIN_PASSWORD";
    static final String FIRST_NAME_PROPERTY =
            "ELITEBNB_ADMIN_FIRST_NAME";
    static final String LAST_NAME_PROPERTY =
            "ELITEBNB_ADMIN_LAST_NAME";

    private static final Logger logger =
            LoggerFactory.getLogger(DevelopmentAdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DevelopmentAdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    /**
     * Runs after the Spring context starts and inserts one ADMIN only when the
     * bootstrap flag is enabled and no Admin account already exists.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (!isBootstrapEnabled()) {
            return;
        }

        String email =
                requiredProperty(EMAIL_PROPERTY);
        String rawPassword =
                requiredProperty(PASSWORD_PROPERTY);

        // The configured email may already belong to a real USER, HOST, or
        // ADMIN. In every case, bootstrap must leave the account untouched.
        if (userRepository.existsByEmail(email)) {
            logger.info(
                    "Development admin bootstrap skipped because the configured account already exists"
            );
            return;
        }

        // This is a first-admin bootstrap, not an Admin management feature.
        // Once any ADMIN exists, a different configured email is ignored.
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            logger.info(
                    "Development admin bootstrap skipped because an admin account already exists"
            );
            return;
        }

        User admin =
                User.builder()
                        .firstName(
                                optionalProperty(
                                        FIRST_NAME_PROPERTY,
                                        "Development"
                                )
                        )
                        .lastName(
                                optionalProperty(
                                        LAST_NAME_PROPERTY,
                                        "Admin"
                                )
                        )
                        .email(email)
                        .password(
                                passwordEncoder.encode(rawPassword)
                        )
                        .role(Role.ADMIN)
                        .accountStatus(AccountStatus.ACTIVE)
                        .emailVerified(true)
                        .build();

        userRepository.save(admin);

        logger.info("Development admin bootstrap completed");
    }

    private boolean isBootstrapEnabled() {

        return Boolean.parseBoolean(
                environment.getProperty(
                        ENABLED_PROPERTY,
                        "false"
                )
        );
    }

    private String requiredProperty(
            String propertyName
    ) {

        String value =
                trimToNull(
                        environment.getProperty(propertyName)
                );

        if (value == null) {
            throw new IllegalStateException(
                    "Development admin bootstrap requires "
                            + propertyName
                            + " when "
                            + ENABLED_PROPERTY
                            + " is true"
            );
        }

        return value;
    }

    private String optionalProperty(
            String propertyName,
            String defaultValue
    ) {

        String value =
                trimToNull(
                        environment.getProperty(propertyName)
                );

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    private String trimToNull(
            String value
    ) {

        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
