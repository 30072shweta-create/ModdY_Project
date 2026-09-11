package com.example.flight.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.flight.model.Domain;

@Service
public class KnowledgeService {

    private final Map<Domain, String> knowledgeCache = new ConcurrentHashMap<>();

    public String getKnowledge(Domain domain) {
        if (domain == null) {
            return getAllKnowledge();
        }
        return knowledgeCache.computeIfAbsent(domain, d -> {
            String mainKnowledge = readFile("knowledge/" + d.getFileName());
            String faqKnowledge = readFile("faq/" + d.getFaqFileName());

            return """
                    === DOMAIN: %s ===
                    MAIN KNOWLEDGE:
                    %s

                    FREQUENTLY ASKED QUESTIONS:
                    %s
                    """.formatted(d.getDisplayName(), mainKnowledge, faqKnowledge);
        });
    }

    public String getAllKnowledge() {
        StringBuilder sb = new StringBuilder();
        for (Domain d : Domain.values()) {
            sb.append(getKnowledge(d)).append("\n\n");
        }
        return sb.toString();
    }

    private String readFile(String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
