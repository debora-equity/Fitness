package com.softgenia.playlist.service;

import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscriptionResponseDto;
import com.softgenia.playlist.model.dto.userSubscription.UserSubscritionFilterDto;
import com.softgenia.playlist.model.entity.UserSubscription;
import com.softgenia.playlist.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {
    private final UserSubscriptionRepository repository;

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


}
