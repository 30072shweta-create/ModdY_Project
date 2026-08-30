package com.example.flight.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.flight.model.Domain;

@Service
public class KnowledgeService {

    public String getKnowledge(Domain domain) {
        String mainKnowledge = readFile("knowledge/" + domain.getFileName());
        String faqKnowledge = readFile("faq/" + domain.getFaqFileName());

        return """
                MAIN KNOWLEDGE:
                %s

                FREQUENTLY ASKED QUESTIONS:
                %s
                """.formatted(mainKnowledge, faqKnowledge);
    }

    private String readFile(String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load knowledge file: " + filePath, e);
        }
    }
}
