package com.softgenia.playlist.service;

import com.softgenia.playlist.model.dto.ManualSubscriptionRequest;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscriptionResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscritionFilterDto;
import com.softgenia.playlist.model.entity.*;
import com.softgenia.playlist.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {
    private final UserSubscriptionRepository repository;
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final PlanRepository planRepository;

    public PageResponseDto<UserSubscriptionResponseDto> getUserSubscription(UserSubscritionFilterDto filterDto, Integer pageNumber, Integer pageSize) {
        var pageable = PageRequest.of(pageNumber, pageSize);
        filterDto.formatData();
        var page = repository.getUserSubscriptions(filterDto, pageable);
        List<UserSubscriptionResponseDto> mappedData = page.stream().map(UserSubscriptionResponseDto::new).toList();
        return new PageResponseDto<UserSubscriptionResponseDto>().ofPage(page, mappedData);
    }

    public List<UserSubscription> getAllForExport(UserSubscritionFilterDto filterDto) {
        filterDto.formatData();
        return repository.getUserSubscriptionsForExport(filterDto);
    }

    public void exportCsv(UserSubscritionFilterDto filterDto, Writer writer) throws IOException {

        List<UserSubscription> list = getAllForExport(filterDto);

        try (CSVPrinter csvPrinter = new CSVPrinter(writer,
                CSVFormat.DEFAULT.withHeader(
                        "ID",
                        "Name",
                        "Surname",
                        "Plan",
                        "Start Date",
                        "Expiry Date",
                        "Active",
                        "Workout",
                        "E-Book"
                ))) {

            for (UserSubscription us : list) {
                csvPrinter.printRecord(
                        us.getId(),
                        us.getUser() != null ? us.getUser().getName() : null,
                        us.getUser() != null ? us.getUser().getSurname() : null,
                        us.getPlan() != null ? us.getPlan().getName() : null,
                        us.getStartDate(),
                        us.getExpiryDate(),
                        us.isActive(),
                        us.getWorkout() != null ? us.getWorkout().getName() : null,
                        us.getDocument() != null ? us.getDocument().getOriginalFilename() : null
                );
            }
        }
    }

    @Transactional
    public void grantManualAccess(ManualSubscriptionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        UserSubscription subscription = null;

        // Default logic: 1 month for Workouts/Plans, 6 months for Documents
        int durationMonths = 1;

        // 1. Handle Plan
        if (request.getPlanId() != null) {
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan not found"));

            subscription = repository.findByUserAndPlan(user, plan)
                    .orElse(new UserSubscription());
            subscription.setPlan(plan);
            durationMonths = 1;
        }
        // 2. Handle Workout
        else if (request.getWorkoutId() != null) {
            Workout workout = workoutRepository.findById(request.getWorkoutId())
                    .orElseThrow(() -> new RuntimeException("Workout not found"));

            subscription = repository.findByUserAndWorkout(user, workout)
                    .orElse(new UserSubscription());
            subscription.setWorkout(workout);
            durationMonths = 1;
        }
        // 3. Handle Document (PDF)
        else if (request.getDocumentId() != null) {
            SharedDocument document = userDocumentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            // Note: Ensure you have findByUserAndDocument in your repository
            subscription = repository.findByUserAndDocument(user, document)
                    .orElse(new UserSubscription());

            subscription.setDocument(document);

            // --- SET DEFAULT TO 6 MONTHS FOR DOCUMENTS ---
            durationMonths = 6;
        } else {
            throw new IllegalArgumentException("You must provide a planId, workoutId, or documentId");
        }

        // 4. Check if Admin manually overrode the duration
        if (request.getDurationInMonths() != null && request.getDurationInMonths() > 0) {
            durationMonths = request.getDurationInMonths();
        }

        // 5. Update Dates
        LocalDateTime now = LocalDateTime.now();
        subscription.setUser(user);

        if (subscription.getId() == null || !subscription.isActive()) {
            // New or Expired: Start from NOW
            subscription.setStartDate(now);
            subscription.setExpiryDate(now.plusMonths(durationMonths));
        } else {
            // Already Active: Extend from the CURRENT expiry date
            subscription.setExpiryDate(subscription.getExpiryDate().plusMonths(durationMonths));
        }

        repository.save(subscription);
    }
}
