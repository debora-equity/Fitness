package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscriptionResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscritionFilterDto;
import com.softgenia.playlist.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            ){
        var page = userSubscriptionService.getUserSubscription(filterDto,pageNumber,pageSize);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

}
