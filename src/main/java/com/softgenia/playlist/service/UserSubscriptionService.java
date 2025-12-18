package com.softgenia.playlist.service;

import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscriptionResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscritionFilterDto;
import com.softgenia.playlist.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {
    private final UserSubscriptionRepository repository;

    public PageResponseDto<UserSubscriptionResponseDto> getUserSubscription(UserSubscritionFilterDto filterDto, Integer pageNumber, Integer pageSize) {
        var pageable = PageRequest.of(pageNumber, pageSize);
        filterDto.formatData();
        var page = repository.getUserSubscriptions(filterDto,pageable);
        List<UserSubscriptionResponseDto> mappedData = page.stream().map(UserSubscriptionResponseDto::new).toList();
        return new PageResponseDto<UserSubscriptionResponseDto>().ofPage(page, mappedData);
    }

}
