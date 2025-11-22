//package com.softgenia.playlist.service.gateway;
//
//package com.softgenia.playlist.service.gateway;
//
//import com.softgenia.playlist.model.dto.payment.PaymentGatewayService;
//import com.softgenia.playlist.model.dto.payment.TransactionResult;
//import com.softgenia.playlist.model.entity.User;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Service;
//import java.math.BigDecimal;
//import java.util.UUID;
//
//@Service
//@Profile("!production") // Active when the 'production' profile is NOT set
//public class MockPaymentGatewayServiceImpl implements PaymentGatewayService {
//
//    @Override
//    public TransactionResult charge(BigDecimal amount, String currency, String paymentNonce, User user) {
//        System.out.println("--- MOCK PAYMENT GATEWAY ---");
//        System.out.println("Charging " + amount + " " + currency + " for user " + user.getUsername());
//
//        // Simulate a failure for testing if the nonce is "fail"
//        if ("fail".equalsIgnoreCase(paymentNonce)) {
//            System.out.println("--- MOCK PAYMENT FAILED ---");
//            return new TransactionResult(false, null, "Mock payment failed by request.");
//        }
//
//        // Simulate success
//        String fakeTransactionId = "mock_txn_" + UUID.randomUUID().toString();
//        System.out.println("--- MOCK PAYMENT SUCCEEDED --- Transaction ID: " + fakeTransactionId);
//        return new TransactionResult(true, fakeTransactionId, null);
//    }
//}
