package com.serviceops.common.businesscode;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BusinessCodeConfiguration {
    public static final String CLOCK_BEAN = "businessCodeClock";

    @Bean(CLOCK_BEAN)
    Clock businessCodeClock() {
        return Clock.systemUTC();
    }
}
