package com.example.flight.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.model.ChatRequest;
import com.example.flight.model.ChatResponse;
import com.example.flight.model.DomainResponse;
import com.example.flight.service.AIChatService;
import com.example.flight.service.DomainService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {

    private final AIChatService aiChatService;
    private final DomainService domainService;

    @GetMapping("/domains")
    public ResponseEntity<List<DomainResponse>> getDomains() {
        return ResponseEntity.ok(domainService.getAllDomains());
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request));
    }
}
