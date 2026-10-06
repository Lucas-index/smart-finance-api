package com.smartapi.service;

import com.smartapi.model.AuditLog;
import com.smartapi.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(AuditLog.Source source, String action, String details) {
        repository.save(new AuditLog(source, action, details));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(Pageable pageable) {
        return repository.findAllByOrderByIdDesc(pageable);
    }
}
