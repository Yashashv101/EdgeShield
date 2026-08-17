package com.shieldgate.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@RestController
@RequestMapping("/api/**")
public class ProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${proxy.target-url}")
    private String targetUrl;

    @RequestMapping
    public ResponseEntity<byte[]> proxy(@RequestBody(required = false) byte[] body, HttpServletRequest request) {
        String path = request.getRequestURI().replaceFirst("^/api", "");
        String queryString = request.getQueryString();
        String url = targetUrl + path + (queryString != null ? "?" + queryString : "");
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            if (!HttpHeaders.HOST.equalsIgnoreCase(headerName) && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)) {
                headers.addAll(headerName, Collections.list(request.getHeaders(headerName)));
            }
        });
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(url, HttpMethod.valueOf(request.getMethod()), entity, byte[].class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).headers(e.getResponseHeaders()).body(e.getResponseBodyAsByteArray());
        }
    }
}