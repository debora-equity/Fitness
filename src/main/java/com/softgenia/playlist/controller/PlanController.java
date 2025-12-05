package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.plan.CreatePlanDto;
import com.softgenia.playlist.model.dto.plan.PlanMinResponseDto;
import com.softgenia.playlist.model.dto.plan.PlanResponseDto;
import com.softgenia.playlist.model.dto.plan.UpdatePlanDto;
import com.softgenia.playlist.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<PageResponseDto<PlanResponseDto>> getAllPlans(
            @RequestParam Integer pageSize,
            @RequestParam Integer pageNumber
    ) {
        var page = planService.getAllPlans(pageNumber, pageSize);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanMinResponseDto> getPlan(@PathVariable Integer id, Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(planService.getPlanById(id, username));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_CREATOR')")
    public ResponseEntity<Void> createPlan(@RequestBody @Valid CreatePlanDto dto) {
        planService.createPlan(dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_CREATOR')")
    public ResponseEntity<Void> updatePlan(@RequestBody @Valid UpdatePlanDto dto) {
        planService.updatePlan(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_CREATOR')")
    public ResponseEntity<?> deletePlan(@PathVariable Integer id) {
        try {
            planService.deletePlan(id);
            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException e) {
            // --- CATCH FOREIGN KEY ERROR ---
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cannot delete this plan because it has been purchased by users."));

        } catch (RuntimeException e) {
            // Plan not found
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}