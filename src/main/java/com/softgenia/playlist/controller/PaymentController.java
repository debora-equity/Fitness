package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.dto.payment.PaymentRequestDto;
import com.softgenia.playlist.service.PayseraService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PayseraService payseraService;

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> initiatePayment(
            @RequestBody PaymentRequestDto request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            String redirectUrl = payseraService.createRedirectUrl(request, username);
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/paysera-callback")
    public ResponseEntity<String> payseraCallback(HttpServletRequest request) {
        payseraService.handleCallback(request);

        return ResponseEntity.ok("OK");
    }
}
