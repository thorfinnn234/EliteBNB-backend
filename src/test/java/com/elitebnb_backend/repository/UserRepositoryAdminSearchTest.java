package com.elitebnb_backend.repository;

import com.elitebnb_backend.dto.AdminUserResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.service.AdminUserService;
import com.elitebnb_backend.service.AuditLogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class UserRepositoryAdminSearchTest {

    @Autowired
    private UserRepository userRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService =
                new AdminUserService(
                        userRepository,
                        mock(AuditLogService.class)
                );

        userRepository.saveAll(
                List.of(
                        user(
                                "Marvellous",
                                "Akinbola",
                                "marvellous@example.com",
                                Role.USER,
                                AccountStatus.ACTIVE
                        ),
                        user(
                                "Marvellous",
                                "Host",
                                "marvellous.host@example.com",
                                Role.HOST,
                                AccountStatus.ACTIVE
                        ),
                        user(
                                "Suspended",
                                "Akinbola",
                                "suspended.akinbola@example.com",
                                Role.USER,
                                AccountStatus.SUSPENDED
                        ),
                        user(
                                "Ada",
                                "Lovelace",
                                "ada@example.com",
                                Role.ADMIN,
                                AccountStatus.ACTIVE
                        )
                )
        );
        userRepository.flush();
    }

    @Test
    void firstNameSearchWorks() {

        assertThat(emailsFor(
                "Marvellous",
                null,
                null
        )).containsExactlyInAnyOrder(
                "marvellous@example.com",
                "marvellous.host@example.com"
        );
    }

    @Test
    void lastNameSearchWorks() {

        assertThat(emailsFor(
                "Akinbola",
                null,
                null
        )).containsExactlyInAnyOrder(
                "marvellous@example.com",
                "suspended.akinbola@example.com"
        );
    }

    @Test
    void fullNameSearchWorksWithTrimmedWhitespace() {

        assertThat(emailsFor(
                "  Marvellous Akinbola  ",
                null,
                null
        )).containsExactly(
                "marvellous@example.com"
        );
    }

    @Test
    void searchIsCaseInsensitive() {

        assertThat(emailsFor(
                "MARVELLOUS",
                null,
                null
        )).containsExactlyInAnyOrder(
                "marvellous@example.com",
                "marvellous.host@example.com"
        );
    }

    @Test
    void emailSearchStillWorks() {

        assertThat(emailsFor(
                "marvellous@example.com",
                null,
                null
        )).containsExactly(
                "marvellous@example.com"
        );
    }

    @Test
    void searchComposesWithRoleFilter() {

        assertThat(emailsFor(
                "Marvellous",
                Role.USER,
                null
        )).containsExactly(
                "marvellous@example.com"
        );
    }

    @Test
    void searchComposesWithAccountStatusFilter() {

        assertThat(emailsFor(
                "Akinbola",
                null,
                AccountStatus.ACTIVE
        )).containsExactly(
                "marvellous@example.com"
        );
    }

    @Test
    void unrelatedNamesDoNotMatch() {

        assertThat(emailsFor(
                "Grace Hopper",
                null,
                null
        )).isEmpty();
    }

    private List<String> emailsFor(
            String search,
            Role role,
            AccountStatus status
    ) {

        return adminUserService
                .getUsers(
                        search,
                        role,
                        status
                )
                .stream()
                .map(AdminUserResponse::getEmail)
                .toList();
    }

    private User user(
            String firstName,
            String lastName,
            String email,
            Role role,
            AccountStatus accountStatus
    ) {

        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("encoded-password")
                .role(role)
                .accountStatus(accountStatus)
                .emailVerified(true)
                .build();
    }
}
