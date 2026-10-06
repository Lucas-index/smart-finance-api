package com.smartapi.controller;

import com.smartapi.model.AuditLog;
import com.smartapi.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    public record AuditEntry(Long id, AuditLog.Source source, String action, String details, Instant createdAt) {
        static AuditEntry from(AuditLog l) {
            return new AuditEntry(l.getId(), l.getSource(), l.getAction(), l.getDetails(), l.getCreatedAt());
        }
    }

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Page<AuditEntry> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "50") int size) {
        return auditService.list(PageRequest.of(page, Math.min(size, 200))).map(AuditEntry::from);
    }
}
