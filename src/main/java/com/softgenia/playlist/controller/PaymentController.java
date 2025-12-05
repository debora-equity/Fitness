package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.dto.payment.PaymentRequestDto;
import com.softgenia.playlist.service.PayseraService;
import com.softgenia.playlist.service.StripeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PayseraService payseraService;
    private final StripeService stripeService;

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> initiatePayment(
            @RequestBody PaymentRequestDto request,
            Authentication authentication) {

        String username = authentication.getName();
        Map<String, String> response = new HashMap<>();

        try {
            String provider = request.getProvider();

            if (provider == null || provider.trim().isEmpty()) {
                throw new IllegalArgumentException("The 'provider' field is required. Values allowed: STRIPE, PAYSERA");
            }

            if ("STRIPE".equalsIgnoreCase(provider)) {
                String sessionUrl = stripeService.createCheckoutSession(request, username);
                response.put("redirectStripeUrl", sessionUrl);

            } else if ("PAYSERA".equalsIgnoreCase(provider)) {
                String redirectUrl = payseraService.createRedirectUrl(request, username);
                response.put("redirectUrl", redirectUrl);

            } else {
                throw new IllegalArgumentException("Invalid provider: " + provider + ". Only STRIPE and PAYSERA are supported.");
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @RequestMapping(value = "/paysera-callback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> payseraCallback(HttpServletRequest request) {
        payseraService.handleCallback(request);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/stripe-webhook")
    public ResponseEntity<String> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook Error");
        }
    }

    @PostMapping("/stripe-confirm")
    public ResponseEntity<String> confirmStripePayment(@RequestParam("session_id") String sessionId) {
        try {
            boolean success = stripeService.confirmPaymentManually(sessionId);
            if (success) {
                return ResponseEntity.ok("Payment Confirmed");
            } else {
                return ResponseEntity.badRequest().body("Payment not paid yet.");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
