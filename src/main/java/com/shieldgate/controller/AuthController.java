package com.shieldgate.controller;

import com.shieldgate.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final JwtService jwtService;

    @Value("${auth.login-url}")
    private String loginUrl;

    @Value("${auth.register-url}")
    private String registerUrl;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            ResponseEntity<String> backendResponse = restTemplate.postForEntity(
                loginUrl, body, String.class);

            if (backendResponse.getStatusCode().is2xxSuccessful()) {
                String token = jwtService.generateToken(body.get("username"));
                return ResponseEntity.ok(Map.of("token", token));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));

        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            ResponseEntity<String> backendResponse = restTemplate.postForEntity(
                registerUrl, body, String.class);

            if (backendResponse.getStatusCode().is2xxSuccessful()) {
                String token = jwtService.generateToken(body.get("username"));
                return ResponseEntity.ok(Map.of(
                    "token", token,
                    "message", "Registration successful"
                ));
            }

            return ResponseEntity.badRequest()
                .body(Map.of("error", "Registration failed"));

        } catch (HttpClientErrorException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}