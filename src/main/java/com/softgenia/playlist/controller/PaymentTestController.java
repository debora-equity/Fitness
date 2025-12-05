package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.entity.Payment;
import com.softgenia.playlist.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class PaymentTestController {

    private final PaymentRepository paymentRepository;

    @Value("${paysera.sign_password}")
    private String signPassword;

    @Value("${paysera.projectid}")
    private String projectId;

    @GetMapping("/api/test/generate-success-callback")
    public ResponseEntity<Map<String, String>> generateCallbackData(@RequestParam String orderId) {
        try {

            Payment payment = paymentRepository.findByTransactionId(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));


            long amountInCents = payment.getAmount().multiply(new BigDecimal("100")).longValue();


            Map<String, String> params = new HashMap<>();
            params.put("projectid", projectId);
            params.put("orderid", orderId);
            params.put("amount", String.valueOf(amountInCents));
            params.put("currency", payment.getCurrency());
            params.put("status", "1");
            params.put("test", "1");


            String queryString = params.entrySet().stream()
                    .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                    .collect(Collectors.joining("&"));


            String data = Base64.getUrlEncoder().encodeToString(queryString.getBytes(StandardCharsets.UTF_8));


            String ss2 = generateMd5(data + signPassword);


            return ResponseEntity.ok(Map.of(
                    "data", data,
                    "ss2", ss2,
                    "orderid", orderId
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String generateMd5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
