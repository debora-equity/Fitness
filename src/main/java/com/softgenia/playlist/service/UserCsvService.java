package com.softgenia.playlist.service;


import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCsvService {

    private final UserRepository userRepository;

    public void writeUsersToCsv(Writer writer) throws IOException {
        List<User> users = userRepository.findAll();

        try (CSVPrinter csvPrinter = new CSVPrinter(writer,
                CSVFormat.DEFAULT.withHeader(
                        "ID",
                        "Username",
                        "Name",
                        "Surname",
                        "Email",
                        "Role"
                ))) {

            for (User user : users) {
                csvPrinter.printRecord(
                        user.getId(),
                        user.getUsername(),
                        user.getName(),
                        user.getSurname(),
                        user.getEmail(),
                        user.getRole() != null ? user.getRole().getName() : null
                );
            }
        }
    }
}
