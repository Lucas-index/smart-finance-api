package com.smartapi.exception;

/** Falha ao falar com o modelo de IA (chave inválida, limite de uso, erro de função etc.). */
public class AssistantException extends RuntimeException {

    public AssistantException(String message, Throwable cause) {
        super(message, cause);
    }
}
