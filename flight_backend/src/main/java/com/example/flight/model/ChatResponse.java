package com.example.flight.model;

public class ChatResponse {

    private String conversationId;
    private Domain domain;
    private String answer;

    public ChatResponse() {
    }

    public ChatResponse(String conversationId, Domain domain, String answer) {
        this.conversationId = conversationId;
        this.domain = domain;
        this.answer = answer;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
