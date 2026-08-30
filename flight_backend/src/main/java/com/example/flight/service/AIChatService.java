package com.example.flight.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import com.example.flight.model.ChatRequest;
import com.example.flight.model.ChatResponse;
import com.example.flight.tools.FlightTools;

import main.java.com.example.flight.tools.BookingTools;
import main.java.com.example.flight.tools.PaymentTools;
import main.java.com.example.flight.tools.CouponTools;
import main.java.com.example.flight.tools.RefundTools;

@Service
public class AIChatService {

    private final ChatClient chatClient;
    private final KnowledgeService knowledgeService;
    private final FlightTools flightTools;
    private final BookingTools bookingTools;
    private final PaymentTools paymentTools;
    private final CouponTools couponTools;
    private final RefundTools refundTools;

    public AIChatService(
        ChatClient.Builder chatClientBuilder,
        KnowledgeService knowledgeService,
        ChatMemory chatMemory,
        FlightTools flightTools,BookingTools bookingTools,PaymentTools paymentTools,CouponTools couponTools,RefundTools refundTools) { 

        this.chatClient = chatClientBuilder
        .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory)
                        .build()
        )
        .defaultTools(flightTools, bookingTools, paymentTools, couponTools, refundTools )
        .build();
    }

    public ChatResponse chat(ChatRequest request) {
        String knowledge = knowledgeService.getKnowledge(request.getDomain());

        String systemPrompt = """
                You are an AI assistant for a Flight Management System.

             There are two types of questions:

               1. STATIC QUESTIONS:
              Use the provided knowledge to answer questions about system rules,
         processes, policies, and general information.

           2. LIVE QUESTIONS:
        For questions requiring current database information, use the
         available tools.

       Examples of live questions include:
       - What flights are available?
        - What flights are available from one airport to another?
      - What is the current flight price?
        - What is the current flight availability?

          Never invent live flight information.
        Use the available tools when current database data is required.
                """.formatted(request.getDomain().getDisplayName(), knowledge);

        String answer = chatClient
                .prompt()
                .system(systemPrompt)
                .user(request.getMessage())
                .advisors(advisorSpec ->
                        advisorSpec.param(ChatMemory.CONVERSATION_ID, request.getConversationId())
                )
                .call()
                .content();

        return new ChatResponse(request.getConversationId(), request.getDomain(), answer);
    }
}
