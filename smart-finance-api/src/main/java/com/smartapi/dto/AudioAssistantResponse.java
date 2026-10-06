package com.smartapi.dto;

/** Resultado do fluxo áudio -> texto -> assistente. */
public record AudioAssistantResponse(String conversationId, String transcription, String reply) {
}
