package io.continuum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The current time is I/O like any other: an imperative shell reads it from this {@link Clock}
 * and hands it to the functional core as a plain value, so the core never calls
 * {@code Instant.now()} itself and tests can pin it with {@link Clock#fixed}.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
