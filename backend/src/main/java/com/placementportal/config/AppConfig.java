package com.placementportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /**
     * Services ask this Clock for "now" instead of calling Instant.now() directly.
     * In unit tests we replace it with a fixed clock, which makes timer logic testable.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
