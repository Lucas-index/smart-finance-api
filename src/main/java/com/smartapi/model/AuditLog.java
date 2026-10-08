package com.smartapi.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Trilha de auditoria: registra o que foi feito pela API e pelas tools da IA. */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    public enum Source { API, AI_TOOL, AUDIO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 2000)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
    }

    public AuditLog(Long userId, Source source, String action, String details) {
        this.userId = userId;
        this.source = source;
        this.action = action;
        this.details = details != null && details.length() > 2000 ? details.substring(0, 2000) : details;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Source getSource() { return source; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
