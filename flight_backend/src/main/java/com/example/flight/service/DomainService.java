package com.example.flight.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.flight.model.Domain;
import com.example.flight.model.DomainResponse;

@Service
public class DomainService {

    public List<DomainResponse> getAllDomains() {
        return Arrays.stream(Domain.values())
                .map(domain -> new DomainResponse(domain.getDisplayName(), domain))
                .toList();
    }
}
