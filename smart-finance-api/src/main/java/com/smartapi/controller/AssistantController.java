package com.smartapi.controller;

import com.smartapi.ai.AssistantService;
import com.smartapi.ai.ChatModelPlayground;
import com.smartapi.ai.SpeechService;
import com.smartapi.dto.ChatRequest;
import com.smartapi.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final SpeechService speechService;
    private final ChatModelPlayground playground;

    public AssistantController(AssistantService assistantService,
                               SpeechService speechService,
                               ChatModelPlayground playground) {
        this.assistantService = assistantService;
        this.speechService = speechService;
        this.playground = playground;
    }

    /** Chat com memória e tools (ChatClient). */
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String id = request.conversationIdOrDefault();
        return new ChatResponse(id, assistantService.chat(id, request.message()));
    }

    /** Chat + voz: responde em áudio (mp3) com a resposta do assistente. */
    @PostMapping(path = "/chat/voice", produces = "audio/mpeg")
    public ResponseEntity<byte[]> chatVoice(@Valid @RequestBody ChatRequest request) {
        String id = request.conversationIdOrDefault();
        String reply = assistantService.chat(id, request.message());
        return audio(speechService.synthesize(reply));
    }

    /** Speech API pura: texto -> mp3. */
    @PostMapping(path = "/speech", produces = "audio/mpeg")
    public ResponseEntity<byte[]> speech(@RequestBody Map<String, String> body) {
        return audio(speechService.synthesize(body.get("text")));
    }

    /** ChatModel direto, sem memória nem tools (para estudo). */
    @GetMapping("/playground")
    public Map<String, String> playground(@RequestParam String question,
                                          @RequestParam(required = false) Double temperature) {
        return Map.of("answer", playground.ask(question, temperature));
    }

    private static ResponseEntity<byte[]> audio(byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resposta.mp3\"")
                .body(bytes);
    }
}
