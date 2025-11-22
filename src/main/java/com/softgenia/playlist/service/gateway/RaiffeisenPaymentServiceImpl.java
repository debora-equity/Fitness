//package com.softgenia.playlist.service.gateway;
//
//import com.softgenia.playlist.model.dto.payment.PaymentGatewayService;
//import com.softgenia.playlist.model.dto.payment.TransactionResult;
//import com.softgenia.playlist.model.entity.User;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Service;
//import java.math.BigDecimal;
//
//@Service
//@Profile("production") // Active ONLY when the 'production' profile is set
//public class RaiffeisenPaymentServiceImpl implements PaymentGatewayService {
//
//    @Override
//    public TransactionResult charge(BigDecimal amount, String currency, User user) {
//        return null;
//    }
//
//    @Override
//    public TransactionResult charge(BigDecimal amount, String currency, String paymentNonce, User user) {
//        // TODO: Implement this method using the official Raiffeisen Bank Java SDK.
//        // You will get the necessary libraries (pom.xml dependency) and API keys
//        // from their developer documentation.
//
//        System.err.println("!!! REAL Raiffeisen Payment Gateway IS NOT IMPLEMENTED YET !!!");
//        return new TransactionResult(false, null, "Payment provider not configured.");
//    }
//}
