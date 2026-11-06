package com.example.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {
    @GetMapping("/fallback")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> fallback() {
        return Map.of("error", "A downstream service is temporarily unavailable");
    }
}
