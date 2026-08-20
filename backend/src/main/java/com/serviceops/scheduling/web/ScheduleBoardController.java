package com.serviceops.scheduling.web;

import com.serviceops.scheduling.application.ScheduleBoardService;
import com.serviceops.scheduling.web.ScheduleBoardDtos.ScheduleBoardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/schedule-board")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','DISPATCHER')")
public class ScheduleBoardController {
    private final ScheduleBoardService service;

    @GetMapping
    public ScheduleBoardResponse getBoard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return service.getBoard(from, to);
    }
}
