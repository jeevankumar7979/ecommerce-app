package com.example.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.security.Principal;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitKeyResolver {
    @Bean
    KeyResolver clientKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                        .map(address -> address.getAddress().getHostAddress())
                        .defaultIfEmpty("anonymous"));
    }
}
