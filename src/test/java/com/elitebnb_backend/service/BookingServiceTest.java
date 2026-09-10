package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.BookingResponse;
import com.elitebnb_backend.dto.CreateBookingRequest;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.PropertyAvailabilityRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PropertyAvailabilityRepository availabilityRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookingService bookingService;

    private User guest;
    private User host;
    private Property property;

    @BeforeEach
    void setUp() {

        guest = User.builder()
                .id(1L)
                .firstName("Habeeb")
                .lastName("Guest")
                .email("guest@test.com")
                .build();

        host = User.builder()
                .id(2L)
                .firstName("Oreoluwa")
                .lastName("Host")
                .email("host@test.com")
                .build();

        property = Property.builder()
                .id(10L)
                .title("Elite Lagos Apartment")
                .host(host)
                .maxGuests(4)
                .pricePerNight(50000.0)
                .build();
    }

    @Test
    void shouldCreateBookingSuccessfully() {

        CreateBookingRequest request =
                new CreateBookingRequest();

        request.setPropertyId(10L);
        request.setCheckIn(LocalDate.now().plusDays(1));
        request.setCheckOut(LocalDate.now().plusDays(4));
        request.setGuests(2);

        when(authentication.getName())
                .thenReturn("guest@test.com");

        when(userRepository.findByEmail("guest@test.com"))
                .thenReturn(Optional.of(guest));

        when(propertyRepository.findById(10L))
                .thenReturn(Optional.of(property));

        when(bookingRepository.existsOverlappingBooking(
                eq(10L),
                eq(request.getCheckIn()),
                eq(request.getCheckOut()),
                anyList()
        )).thenReturn(false);

        when(availabilityRepository.existsOverlappingBlock(
                10L,
                request.getCheckIn(),
                request.getCheckOut()
        )).thenReturn(false);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking =
                            invocation.getArgument(0);

                    booking.setId(100L);

                    return booking;
                });

        BookingResponse response =
                bookingService.createBooking(
                        request,
                        authentication
                );

        assertNotNull(response);

        assertEquals(
                BookingStatus.PENDING,
                response.getStatus()
        );

        assertEquals(
                150000.0,
                response.getTotalAmount()
        );

        verify(bookingRepository, times(1))
                .save(any(Booking.class));

        verify(notificationService, times(1))
                .createNotification(
                        eq(host),
                        eq("New reservation request"),
                        anyString(),
                        any(),
                        any(Booking.class),
                        eq(property)
                );
    }

    @Test
    void shouldRejectHostBookingOwnProperty() {

        CreateBookingRequest request =
                new CreateBookingRequest();

        request.setPropertyId(10L);
        request.setCheckIn(LocalDate.now().plusDays(1));
        request.setCheckOut(LocalDate.now().plusDays(3));
        request.setGuests(1);

        when(authentication.getName())
                .thenReturn("host@test.com");

        when(userRepository.findByEmail("host@test.com"))
                .thenReturn(Optional.of(host));

        when(propertyRepository.findById(10L))
                .thenReturn(Optional.of(property));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> bookingService.createBooking(
                                request,
                                authentication
                        )
                );

        assertEquals(
                "You cannot book your own property",
                exception.getMessage()
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));

        verify(notificationService, never())
                .createNotification(
                        any(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        any()
                );
    }
}