// com/softgenia/playlist/service/PayseraService.java
package com.softgenia.playlist.service;

//import com.paysera.lib.webtopay.WebToPay;
import com.softgenia.playlist.model.constants.PaymentStatus;
import com.softgenia.playlist.model.dto.payment.PaymentRequestDto;
import com.softgenia.playlist.model.entity.Payment;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.PaymentRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.utils.ManualPayseraClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayseraService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Value("${paysera.projectid}")
    private int projectId;
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


        String orderId = UUID.randomUUID().toString();

        long amountInCents = request.getAmount().multiply(new BigDecimal("100")).longValue();


        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(orderId);
        payment.setCreated(LocalDateTime.now());
        paymentRepository.save(payment);

        try {
            Map<String, String> params = Map.of(
                    "projectid", String.valueOf(projectId),
                    "orderid", orderId,
                    "amount", String.valueOf(amountInCents),
                    "currency", request.getCurrency(),
                    "accepturl", frontendBaseUrl + "/payment/success",
                    "cancelurl", frontendBaseUrl + "/payment/cancel",
                    "callbackurl", backendBaseUrl + "/api/payments/paysera-callback",
                    "test", "1"
            );


            return ManualPayseraClient.buildRequestUrl(params, signPassword);
        } catch (Exception e) {
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
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
            paymentRepository.save(payment);

        } catch (Exception e) {
            System.err.println("Invalid Paysera callback received: " + e.getMessage());
        }
    }
}