package com.smartapi.ai;

import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.stereotype.Service;

/** Speech API: texto -> voz (Text-to-Speech). Voz e formato vêm do application.yml. */
@Service
public class SpeechService {

    private static final int MAX_CHARS = 4000; // limite da API é 4096

    private final OpenAiAudioSpeechModel speechModel;

    public SpeechService(OpenAiAudioSpeechModel speechModel) {
        this.speechModel = speechModel;
    }

    /** Retorna os bytes do áudio (mp3 por padrão). */
    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("O texto para síntese de voz não pode ser vazio.");
        }
        String safe = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
        return speechModel.call(safe);
    }
}
