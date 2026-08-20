package com.serviceops.scheduling.web;

import com.serviceops.scheduling.application.MyScheduleService;
import com.serviceops.scheduling.web.MyScheduleDtos.MyScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/my-schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TECHNICIAN')")
public class MyScheduleController {
    private final MyScheduleService service;

    @GetMapping
    public MyScheduleResponse get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return service.get(from, to);
    }
}
