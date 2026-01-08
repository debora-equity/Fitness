package com.softgenia.playlist.service;

import com.softgenia.playlist.model.constants.PaymentProvider;
import com.softgenia.playlist.model.constants.PaymentStatus;
import com.softgenia.playlist.model.dto.payment.PaymentRequestDto;
import com.softgenia.playlist.model.entity.*;
import com.softgenia.playlist.repository.*;
import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final WorkoutRepository workoutRepository;
    private final UserDocumentRepository documentRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Transactional
    public String createCheckoutSession(PaymentRequestDto request, String username) throws StripeException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal priceToCharge;
        String productName;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", user.getId().toString());

        Payment payment = new Payment();
        payment.setProvider(PaymentProvider.STRIPE);

        if (request.getPlanId() != null) {
            Plan plan = planRepository.findById(request.getPlanId()).orElseThrow();
            priceToCharge = plan.getPrice();
            productName = "Plan: " + plan.getName();
            metadata.put("planId", plan.getId().toString());
            payment.setPlan(plan);
        } else if (request.getWorkoutId() != null) {
            Workout workout = workoutRepository.findById(request.getWorkoutId()).orElseThrow();
            if (Boolean.TRUE.equals(workout.getIsFree())) {
                throw new IllegalArgumentException("This workout is free. Payment is not required.");
            }
            priceToCharge = workout.getPrice();
            productName = "Workout: " + workout.getName();
            metadata.put("workoutId", workout.getId().toString());
            payment.setWorkout(workout);
        } else if (request.getDocumentId() != null) {
            SharedDocument doc = documentRepository.findById(request.getDocumentId()).orElseThrow();
            priceToCharge = doc.getPrice();
            productName = "Document: " + doc.getOriginalFilename();
            metadata.put("documentId", doc.getId().toString());
            payment.setDocument(doc);
        } else {
            throw new IllegalArgumentException("No product selected");
        }

        long amountInCents = priceToCharge.multiply(new BigDecimal("100")).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCancelUrl(frontendBaseUrl + "/payment/cancel")
                .setSuccessUrl(frontendBaseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCustomerEmail(user.getEmail())
                .putAllMetadata(metadata)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(request.getCurrency().toLowerCase())
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(productName)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();


        Session session = Session.create(params);


        payment.setUser(user);
        payment.setTransactionId(session.getId());
        payment.setAmount(priceToCharge);
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreated(LocalDateTime.now());
        paymentRepository.save(payment);


        return session.getUrl();
    }


    @Transactional
    public void handleWebhook(String payload, String sigHeader) throws EventDataObjectDeserializationException {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            throw new RuntimeException("Webhook signature verification failed");
        }

        System.out.println(">>> STRIPE WEBHOOK RECEIVED: " + event.getType());

        if ("checkout.session.completed".equals(event.getType())) {

            Session session = null;


            var deserializer = event.getDataObjectDeserializer();


            if (deserializer.getObject().isPresent()) {
                session = (Session) deserializer.getObject().get();
            } else {
                System.out.println(">>> SDK Version mismatch. Using deserializeUnsafe()...");
                session = (Session) deserializer.deserializeUnsafe();
            }

            if (session != null) {
                System.out.println(">>> Processing Session ID: " + session.getId());
                fulfillOrder(session);
            } else {
                System.err.println(">>> FAILED: Could not deserialize Session object!");
            }
        } else {
            System.out.println(">>> Ignoring event type: " + event.getType());
        }
    }

    private void fulfillOrder(Session session) {

        String transactionId = session.getId();
        System.out.println(">>> Looking for Payment in DB with Transaction ID: " + transactionId);

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + transactionId));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            System.out.println(">>> Payment " + transactionId + " already processed. Skipping subscription update.");
            return;
        }
        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        Map<String, String> meta = session.getMetadata();
        User user = payment.getUser();
        LocalDateTime now = LocalDateTime.now();

        UserSubscription subscription = null;
        int durationMonths;

        if (meta.containsKey("planId")) {

            Plan plan = planRepository
                    .findById(Integer.parseInt(meta.get("planId")))
                    .orElseThrow(() -> new RuntimeException("Plan not found"));

            subscription = subscriptionRepository
                    .findByUserAndPlan(user, plan)
                    .orElse(new UserSubscription());

            subscription.setPlan(plan);
            durationMonths = 1;

        } else if (meta.containsKey("workoutId")) {

            Workout workout = workoutRepository
                    .findById(Integer.parseInt(meta.get("workoutId")))
                    .orElseThrow(() -> new RuntimeException("Workout not found"));

            subscription = subscriptionRepository
                    .findByUserAndWorkout(user, workout)
                    .orElse(new UserSubscription());

            subscription.setWorkout(workout);
            durationMonths = 1;

        } else if (meta.containsKey("documentId")) {

            SharedDocument document = documentRepository
                    .findById(Integer.parseInt(meta.get("documentId")))
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            subscription = subscriptionRepository
                    .findByUserAndDocument(user, document)
                    .orElse(new UserSubscription());

            subscription.setDocument(document);
            durationMonths = 6;

        } else {
            throw new IllegalStateException("Stripe payment has no associated item");
        }

        subscription.setUser(user);

        if (subscription.getId() == null || !subscription.isActive()) {
            subscription.setStartDate(now);
            subscription.setExpiryDate(now.plusMonths(durationMonths));
        } else {
            subscription.setExpiryDate(subscription.getExpiryDate().plusMonths(durationMonths));
        }

        subscriptionRepository.save(subscription);

        System.out.println(">>> Stripe Payment & Subscription saved. Duration = " + durationMonths + " months");
    }


    @Transactional
    public boolean confirmPaymentManually(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);

            if ("paid".equals(session.getPaymentStatus())) {
                fulfillOrder(session);
                return true;
            }
            return false;
        } catch (StripeException e) {
            throw new RuntimeException("Failed to verify Stripe session", e);
        }
    }
}