package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.dto.ManualSubscriptionRequest;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscriptionResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscritionFilterDto;
import com.softgenia.playlist.service.UserSubscriptionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-subscription")
public class UserSubscriptionController {
    private final UserSubscriptionService userSubscriptionService;

    @GetMapping
    public ResponseEntity<PageResponseDto<UserSubscriptionResponseDto>> getVideo(
            @RequestParam Integer pageSize,
            @RequestParam Integer pageNumber,
            @ModelAttribute UserSubscritionFilterDto filterDto
    ) {
        var page = userSubscriptionService.getUserSubscription(filterDto, pageNumber, pageSize);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @GetMapping("/export/csv")
    public void exportCsv(
            @ModelAttribute UserSubscritionFilterDto filterDto,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=user-subscriptions.csv");

        userSubscriptionService.exportCsv(filterDto, response.getWriter());
    }

    @PostMapping("/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> grantAccess(@Valid @RequestBody ManualSubscriptionRequest request) {
        try {
            userSubscriptionService.grantManualAccess(request);
            return ResponseEntity.ok("Access granted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to grant access: " + e.getMessage());
        }
    }
}
