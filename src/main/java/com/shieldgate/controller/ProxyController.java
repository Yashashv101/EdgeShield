package com.shieldgate.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/**")
public class ProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${proxy.target-url}")
    private String targetUrl;

    @RequestMapping
    public ResponseEntity<String> proxy(HttpServletRequest request) {
        String path = request.getRequestURI().replaceFirst("^/api", "");
        String url = targetUrl + path;

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(request.getMethod()),
                null,
                String.class);

        return response;
    }
}