package com.smartapi.controller;

import com.smartapi.ai.AssistantService;
import com.smartapi.ai.TranscriptionService;
import com.smartapi.dto.AudioAssistantResponse;
import com.smartapi.dto.BalanceResponse;
import com.smartapi.dto.TransactionRequest;
import com.smartapi.dto.TransactionResponse;
import com.smartapi.model.AuditLog;
import com.smartapi.service.AuditService;
import com.smartapi.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TranscriptionService transcriptionService;
    private final AssistantService assistantService;
    private final AuditService auditService;

    public TransactionController(TransactionService transactionService,
                                 TranscriptionService transcriptionService,
                                 AssistantService assistantService,
                                 AuditService auditService) {
        this.transactionService = transactionService;
        this.transcriptionService = transcriptionService;
        this.assistantService = assistantService;
        this.auditService = auditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return transactionService.create(request, AuditLog.Source.API);
    }

    @GetMapping
    public Page<TransactionResponse> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return transactionService.list(PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        return transactionService.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        transactionService.delete(id);
    }

    @GetMapping("/balance")
    public BalanceResponse balance() {
        return transactionService.balance();
    }

    /**
     * Endpoint de transcrição: o usuário envia um áudio ("gastei 45 reais no almoço"),
     * o Whisper transcreve e o assistente registra a transação via tool calling.
     */
    @PostMapping(path = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AudioAssistantResponse fromAudio(@RequestParam("file") MultipartFile file,
                                            @RequestParam(defaultValue = "default") String conversationId) {
        String text = transcriptionService.transcribe(file);
        auditService.record(AuditLog.Source.AUDIO, "AUDIO_TRANSCRIBED", text);
        String reply = assistantService.chat(conversationId, text, AuditLog.Source.AUDIO);
        return new AudioAssistantResponse(conversationId, text, reply);
    }
}
