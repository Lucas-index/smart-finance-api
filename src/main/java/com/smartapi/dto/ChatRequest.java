package com.smartapi.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message, String conversationId) {

    public String conversationIdOrDefault() {
        return (conversationId == null || conversationId.isBlank()) ? "default" : conversationId;
    }
}
