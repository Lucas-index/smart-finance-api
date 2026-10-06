package com.smartapi.ai;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Transcription API: áudio (mp3, wav, m4a, webm...) -> texto, usando Whisper. */
@Service
public class TranscriptionService {

    private final OpenAiAudioTranscriptionModel transcriptionModel;

    public TranscriptionService(OpenAiAudioTranscriptionModel transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    public String transcribe(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo de áudio no campo 'file'.");
        }
        try {
            final String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.mp3";
            // A OpenAI identifica o formato pela extensão, por isso sobrescrevemos getFilename().
            Resource audio = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            var options = OpenAiAudioTranscriptionOptions.builder()
                    .model("whisper-1")
                    .language("pt")
                    .build();

            return transcriptionModel.call(new AudioTranscriptionPrompt(audio, options))
                    .getResult()
                    .getOutput();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o áudio enviado.", e);
        }
    }
}
