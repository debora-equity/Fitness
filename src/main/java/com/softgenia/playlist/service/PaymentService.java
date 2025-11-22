//// com/softgenia/playlist/service/PaymentService.java
//package com.softgenia.playlist.service;
//
//
//import com.softgenia.playlist.model.constants.PaymentStatus;
//import com.softgenia.playlist.model.dto.payment.PaymentGatewayService;
//import com.softgenia.playlist.model.dto.payment.PaymentRequestDto;
//import com.softgenia.playlist.model.dto.payment.TransactionResult;
//import com.softgenia.playlist.model.entity.Payment;
//import com.softgenia.playlist.model.entity.User;
//import com.softgenia.playlist.repository.PaymentRepository;
//import com.softgenia.playlist.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentService {
//    private final PaymentGatewayService paymentGateway;
//    private final PaymentRepository paymentRepository;
//    private final UserRepository userRepository;
//
//    @Transactional
//    public Payment processPayment(PaymentRequestDto request, String username) {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        TransactionResult result = paymentGateway.charge(
//                request.getAmount(),
//                request.getCurrency(),
//                user
//        );
//
//
//        if (!result.isSuccess()) {
//
//            throw new RuntimeException("Payment failed: " + result.getErrorMessage());
//        }
//
//
//        Payment payment = new Payment();
//        payment.setUser(user);
//        payment.setAmount(request.getAmount());
//        payment.setCurrency(request.getCurrency());
//        payment.setCreated(LocalDateTime.now());
//        payment.setStatus(PaymentStatus.SUCCEEDED);
//        payment.setTransactionId(result.getTransactionId());
//
//
//        return paymentRepository.save(payment);
//    }
//}