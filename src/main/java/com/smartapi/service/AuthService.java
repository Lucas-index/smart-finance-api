package com.smartapi.service;

import com.smartapi.dto.AuthDtos.AuthResponse;
import com.smartapi.dto.AuthDtos.UserResponse;
import com.smartapi.exception.UnauthorizedException;
import com.smartapi.model.AuditLog;
import com.smartapi.model.AuthToken;
import com.smartapi.model.User;
import com.smartapi.repository.AuthTokenRepository;
import com.smartapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final AuditService auditService;
    private final long tokenDays;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users,
                       AuthTokenRepository tokens,
                       AuditService auditService,
                       @Value("${app.auth.token-days:7}") long tokenDays) {
        this.users = users;
        this.tokens = tokens;
        this.auditService = auditService;
        this.tokenDays = tokenDays;
    }

    @Transactional
    public AuthResponse register(String name, String email, String password) {
        String normalized = normalizeEmail(email);
        if (users.existsByEmail(normalized)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
        User user;
        try {
            user = users.saveAndFlush(new User(name.trim(), normalized, encoder.encode(password)));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
        auditService.record(user.getId(), AuditLog.Source.API, "USER_REGISTERED", normalized);
        return issueToken(user);
    }

    @Transactional
    public AuthResponse login(String email, String password) {
        User user = users.findByEmail(normalizeEmail(email))
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha incorretos."));
        tokens.deleteByExpiresAtBefore(Instant.now());   // limpeza de sessões vencidas
        auditService.record(user.getId(), AuditLog.Source.API, "USER_LOGIN", user.getEmail());
        return issueToken(user);
    }

    /** Valida o token e devolve o id do usuário dono dele. */
    @Transactional(readOnly = true)
    public Long authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Faça login para continuar.");
        }
        return tokens.findByTokenHash(hash(token))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .map(AuthToken::getUserId)
                .orElseThrow(() -> new UnauthorizedException("Sessão expirada. Entre novamente."));
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokens.deleteByTokenHash(hash(token));
        }
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId)
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail()))
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado."));
    }

    // ---------- internos ----------

    private AuthResponse issueToken(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new AuthToken(hash(token), user.getId(), Instant.now().plus(Duration.ofDays(tokenDays))));
        return new AuthResponse(token, new UserResponse(user.getId(), user.getName(), user.getEmail()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
