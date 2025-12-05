package com.softgenia.playlist.service;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.plan.CreatePlanDto;
import com.softgenia.playlist.model.dto.plan.PlanMinResponseDto;
import com.softgenia.playlist.model.dto.plan.PlanResponseDto;
import com.softgenia.playlist.model.dto.plan.UpdatePlanDto;
import com.softgenia.playlist.model.entity.Plan;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.model.entity.UserSubscription;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final PlanRepository repository;
    private final WorkoutRepository workoutRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Transactional
    public void createPlan(CreatePlanDto dto) {
        Plan plan = new Plan();
        plan.setName(dto.getName());
        plan.setPrice(dto.getPrice());
        plan.setIsPaid(dto.getIsPaid() != null ? dto.getIsPaid() : false);
        plan.setIsBlocked(false);

        if (dto.getWorkoutIds() != null && !dto.getWorkoutIds().isEmpty()) {
            List<Workout> workouts = workoutRepository.findAllById(dto.getWorkoutIds());
            plan.setWorkouts(new HashSet<>(workouts));
        }
        repository.save(plan);
    }

    @Transactional
    public void updatePlan(UpdatePlanDto dto) {
        Plan plan = repository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + dto.getId()));

        if (dto.getName() != null) plan.setName(dto.getName());
        if (dto.getPrice() != null) plan.setPrice(dto.getPrice());
        if (dto.getIsPaid() != null) plan.setIsPaid(dto.getIsPaid());
        plan.setIsBlocked(dto.getIsBlocked());

        if (dto.getWorkoutIds() != null) {
            List<Workout> newWorkouts = workoutRepository.findAllById(dto.getWorkoutIds());
            plan.getWorkouts().clear();
            plan.getWorkouts().addAll(newWorkouts);
        }
        repository.save(plan);
    }

    @Transactional
    public void deletePlan(Integer id) {
        Plan plan = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        repository.delete(plan);
        repository.flush();
    }


    @Transactional
    public PageResponseDto<PlanResponseDto> getAllPlans(Integer pageNumber, Integer pageSize) {


        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Integer userId = user.getId();

        boolean isAdminOrCreator = user.getRole().getName() == Roles.ROLE_ADMIN ||
                user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR;

        Set<Integer> unlockedPlanIds = new HashSet<>();
        List<UserSubscription> subs = subscriptionRepository.findAll();

        if (!isAdminOrCreator) {
            List<Integer> activeIds = subscriptionRepository.findActivePlanIds(userId, LocalDateTime.now());
            unlockedPlanIds.addAll(activeIds);
        }

        var pageable = PageRequest.of(pageNumber, pageSize);
        var page = repository.findAll(pageable);


        List<PlanResponseDto> mappedData = page.stream().map(plan -> {
            boolean isUnlocked = false;

            if (isAdminOrCreator) {
                isUnlocked = true;
            } else if (unlockedPlanIds.contains(plan.getId())) {
                isUnlocked = true;
            }

            return new PlanResponseDto(plan, isUnlocked);

        }).toList();

        return new PageResponseDto<PlanResponseDto>().ofPage(page, mappedData);
    }

    @Transactional
    public PlanMinResponseDto getPlanById(Integer planId, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer userId = user.getId();

        Plan plan = repository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        boolean isUnlocked = false;

        boolean isAdminOrCreator = user.getRole().getName() == Roles.ROLE_ADMIN ||
                user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR;

        if (isAdminOrCreator) {
            isUnlocked = true;
        } else {

            List<Integer> unlockedPlanIds = subscriptionRepository.findActivePlanIds(
                    userId,
                    LocalDateTime.now()
            );

            if (unlockedPlanIds.contains(planId)) {
                isUnlocked = true;
            }
        }

        if (!isUnlocked) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have an active subscription for this plan.");
        }

        return new PlanMinResponseDto(plan, isUnlocked);
    }


}
