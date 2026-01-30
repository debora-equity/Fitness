package com.softgenia.playlist.controller;


import com.softgenia.playlist.service.UserCsvService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/export")
public class UserExportController {

    private final UserCsvService userCsvService;

    @GetMapping("/users.csv")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void exportUsersToCsv(HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=users.csv"
        );

        userCsvService.writeUsersToCsv(response.getWriter());
    }
}

