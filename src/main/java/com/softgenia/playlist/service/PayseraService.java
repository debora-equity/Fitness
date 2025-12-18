package com.softgenia.playlist.service;

import com.softgenia.playlist.model.constants.PaymentProvider;
import com.softgenia.playlist.model.constants.PaymentStatus;
import com.softgenia.playlist.model.dto.payment.PaymentRequestDto;
import com.softgenia.playlist.model.entity.*;
import com.softgenia.playlist.repository.*;
import com.softgenia.playlist.utils.ManualPayseraClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayseraService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final WorkoutRepository workoutRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserDocumentRepository userDocumentRepository;

    @Value("${paysera.projectid}")
    private String projectId;
    @Value("${paysera.sign_password}")
    private String signPassword;
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;
    @Value("${backend.base-url}")
    private String backendBaseUrl;

    @Transactional
    public String createRedirectUrl(PaymentRequestDto request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);

        String orderId = UUID.randomUUID().toString();
        payment.setTransactionId(orderId);
        payment.setCreated(LocalDateTime.now());
        payment.setProvider(PaymentProvider.PAYSERA);

        BigDecimal priceToCharge;
        if (request.getPlanId() != null) {
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan not found: " + request.getPlanId()));
            if (Boolean.TRUE.equals(plan.getIsBlocked())) {
                throw new IllegalArgumentException("This Plan is no longer available for purchase.");
            }
            payment.setPlan(plan);
            priceToCharge = plan.getPrice();
        } else if (request.getWorkoutId() != null) {
            Workout workout = workoutRepository.findById(request.getWorkoutId())
                    .orElseThrow(() -> new RuntimeException("Workout not found: " + request.getWorkoutId()));
            if (Boolean.TRUE.equals(workout.getIsBlocked())) {
                throw new IllegalArgumentException("This Plan is no longer available for purchase.");
            }
            payment.setWorkout(workout);
            priceToCharge = workout.getPrice();
        } else if (request.getDocumentId() != null) {
            SharedDocument doc = userDocumentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            if (Boolean.TRUE.equals(doc.getIsBlocked())) {
                throw new IllegalArgumentException("This Plan is no longer available for purchase.");
            }
            payment.setDocument(doc);
            priceToCharge = doc.getPrice();
        } else {
            throw new IllegalArgumentException("Payment request must contain either planId, workoutId, or documentId");
        }

        payment.setAmount(priceToCharge);
        paymentRepository.save(payment);

        long amountInCents = priceToCharge.multiply(new BigDecimal("100")).longValue();

        try {

            Map<String, String> params = new HashMap<>();
            params.put("projectid", String.valueOf(projectId));
            params.put("orderid", orderId);
            params.put("amount", String.valueOf(amountInCents));
            params.put("currency", request.getCurrency());
            params.put("accepturl", frontendBaseUrl + "/payment/success");
            params.put("cancelurl", frontendBaseUrl + "/payment/cancel");

            params.put("callbackurl", backendBaseUrl + "/api/payments/paysera-callback");

            params.put("test", "1");

            if (user.getName() != null) {
                params.put("p_firstname", user.getName());
            }

            if (user.getSurname() != null) {
                params.put("p_lastname", user.getSurname());
            }

            if (user.getEmail() != null) {
                params.put("p_email", user.getEmail());
            }
            return ManualPayseraClient.buildRequestUrl(params, signPassword);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to build Paysera redirect URL", e);
        }
    }

    @Transactional
    public void handleCallback(HttpServletRequest request) {
        try {
            Map<String, String> response = ManualPayseraClient.parseResponse(request.getParameterMap(), signPassword);

            String orderId = response.get("orderid");
            String status = response.get("status");

            Payment payment = paymentRepository.findByTransactionId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found for orderId: " + orderId));

            if ("1".equals(status)) {
                payment.setStatus(PaymentStatus.SUCCEEDED);

                User user = payment.getUser();
                LocalDateTime now = LocalDateTime.now();
                UserSubscription subscription = null;
                int durationMonths;

                if (payment.getPlan() != null) {
                    subscription = subscriptionRepository
                            .findByUserAndPlan(user, payment.getPlan())
                            .orElse(new UserSubscription());
                    subscription.setPlan(payment.getPlan());
                    durationMonths = 1;

                } else if (payment.getWorkout() != null) {
                    subscription = subscriptionRepository
                            .findByUserAndWorkout(user, payment.getWorkout())
                            .orElse(new UserSubscription());
                    subscription.setWorkout(payment.getWorkout());
                    durationMonths = 1;

                } else if (payment.getDocument() != null) {
                    subscription = subscriptionRepository
                            .findByUserAndDocument(user, payment.getDocument())
                            .orElse(new UserSubscription());
                    subscription.setDocument(payment.getDocument());
                    durationMonths = 6;

                } else {
                    throw new IllegalStateException("Payment has no purchasable item");
                }

                subscription.setUser(user);

                if (subscription.getId() == null || !subscription.isActive()) {
                    subscription.setStartDate(now);
                    subscription.setExpiryDate(now.plusMonths(durationMonths));
                } else {
                    subscription.setExpiryDate(subscription.getExpiryDate().plusMonths(durationMonths));
                }

                subscriptionRepository.save(subscription);

                System.out.println("Subscription updated for user: " + user.getUsername());

        } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
            paymentRepository.save(payment);

        } catch (Exception e) {
            System.err.println("Invalid Paysera callback received: " + e.getMessage());
        }
    }
}