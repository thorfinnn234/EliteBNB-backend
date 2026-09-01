package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.PaystackInitializeResponse;
import com.elitebnb_backend.dto.PaymentResponse;
import com.elitebnb_backend.entity.*;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.PaymentRepository;
import com.elitebnb_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    @Value("${paystack.base-url}")
    private String paystackBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // =========================
    // INITIALIZE PAYMENT
    // =========================

    public PaystackInitializeResponse initializePayment(
            Long bookingId,
            String email
    ) {

        User user = getUser(email);

        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found")
                );

        if (!booking.getGuest().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You cannot pay for this booking"
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending bookings can be paid for"
            );
        }

        Optional<Payment> existingPayment =
                paymentRepository.findByBooking(booking);

        if (existingPayment.isPresent()) {

            Payment payment = existingPayment.get();

            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                throw new RuntimeException(
                        "This booking has already been paid for"
                );
            }
        }

        BigDecimal amount = BigDecimal
                .valueOf(booking.getTotalAmount())
                .setScale(2, RoundingMode.HALF_UP);

        /*
         * Paystack expects amount in kobo.
         *
         * ₦10,000 = 1,000,000 kobo
         */
        long amountInKobo = amount
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        String reference =
                "ELITEBNB-" +
                        booking.getId() +
                        "-" +
                        UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 12)
                                .toUpperCase();

        // =========================
        // PAYSTACK REQUEST
        // =========================

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setBearerAuth(
                paystackSecretKey
        );

        Map<String, Object> requestBody =
                new HashMap<>();

        requestBody.put(
                "email",
                user.getEmail()
        );

        requestBody.put(
                "amount",
                amountInKobo
        );

        requestBody.put(
                "currency",
                "NGN"
        );

        requestBody.put(
                "reference",
                reference
        );

        /*
         * After Paystack payment, redirect
         * the user back to the React app.
         */
        requestBody.put(
                "callback_url",
                "http://localhost:5173/payment/callback"
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        paystackBaseUrl +
                                "/transaction/initialize",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

        Map<String, Object> responseBody =
                response.getBody();

        if (
                responseBody == null ||
                        !Boolean.TRUE.equals(
                                responseBody.get("status")
                        )
        ) {
            throw new RuntimeException(
                    "Unable to initialize Paystack payment"
            );
        }

        Map<String, Object> data =
                (Map<String, Object>)
                        responseBody.get("data");

        if (data == null) {
            throw new RuntimeException(
                    "Invalid response from Paystack"
            );
        }

        String authorizationUrl =
                String.valueOf(
                        data.get("authorization_url")
                );

        String accessCode =
                String.valueOf(
                        data.get("access_code")
                );

        String paystackReference =
                String.valueOf(
                        data.get("reference")
                );

        // =========================
        // SAVE PAYMENT
        // =========================

        Payment payment;

        if (existingPayment.isPresent()) {

            payment = existingPayment.get();

            payment.setAmount(amount);
            payment.setReference(paystackReference);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setProvider(
                    PaymentProvider.PAYSTACK
            );

        } else {

            payment = Payment.builder()
                    .booking(booking)
                    .user(user)
                    .amount(amount)
                    .currency("NGN")
                    .reference(paystackReference)
                    .status(PaymentStatus.PENDING)
                    .provider(PaymentProvider.PAYSTACK)
                    .build();
        }

        paymentRepository.save(payment);

        return new PaystackInitializeResponse(
                authorizationUrl,
                accessCode,
                paystackReference
        );
    }

    // =========================
    // VERIFY PAYMENT
    // =========================

    public PaymentResponse verifyPayment(
            String reference,
            String email
    ) {

        User user = getUser(email);

        Payment payment = paymentRepository
                .findByReference(reference)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found"
                        )
                );

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You cannot verify this payment"
            );
        }

        /*
         * Prevent double processing.
         */
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return mapToResponse(payment);
        }

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                paystackSecretKey
        );

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        paystackBaseUrl +
                                "/transaction/verify/" +
                                reference,
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

        Map<String, Object> responseBody =
                response.getBody();

        if (
                responseBody == null ||
                        !Boolean.TRUE.equals(
                                responseBody.get("status")
                        )
        ) {
            throw new RuntimeException(
                    "Unable to verify payment"
            );
        }

        Map<String, Object> data =
                (Map<String, Object>)
                        responseBody.get("data");

        if (data == null) {
            throw new RuntimeException(
                    "Invalid verification response from Paystack"
            );
        }

        String paystackStatus =
                String.valueOf(
                        data.get("status")
                );

        if (!"success".equalsIgnoreCase(paystackStatus)) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Payment was not successful"
            );
        }

        // =========================
        // VERIFY AMOUNT
        // =========================

        Object amountObject =
                data.get("amount");

        if (!(amountObject instanceof Number)) {
            throw new RuntimeException(
                    "Invalid payment amount returned by Paystack"
            );
        }

        long paidAmountInKobo =
                ((Number) amountObject).longValue();

        long expectedAmountInKobo =
                payment.getAmount()
                        .multiply(
                                BigDecimal.valueOf(100)
                        )
                        .longValueExact();

        if (paidAmountInKobo != expectedAmountInKobo) {
            throw new RuntimeException(
                    "Payment amount does not match booking amount"
            );
        }

        // =========================
        // PAYMENT SUCCESS
        // =========================

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        payment.setPaidAt(
                LocalDateTime.now()
        );

        paymentRepository.save(payment);

        Booking booking =
                payment.getBooking();

        booking.setStatus(
                BookingStatus.CONFIRMED
        );

        bookingRepository.save(booking);

        return mapToResponse(payment);
    }

    // =========================
    // GET PAYMENT FOR BOOKING
    // =========================

    public PaymentResponse getBookingPayment(
            Long bookingId,
            String email
    ) {

        User user = getUser(email);

        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found"
                        )
                );

        if (!booking.getGuest().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You cannot access this payment"
            );
        }

        Payment payment =
                paymentRepository
                        .findByBooking(booking)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found"
                                )
                        );

        return mapToResponse(payment);
    }

    // =========================
    // USER
    // =========================

    private User getUser(String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        if (user.getRole() != Role.USER) {
            throw new RuntimeException(
                    "Guest account required"
            );
        }

        return user;
    }

    // =========================
    // RESPONSE
    // =========================

    private PaymentResponse mapToResponse(
            Payment payment
    ) {

        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}