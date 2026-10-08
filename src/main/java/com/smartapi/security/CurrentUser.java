package com.smartapi.security;

import com.smartapi.exception.UnauthorizedException;

/**
 * Guarda o id do usuário logado durante a requisição (preenchido pelo AuthInterceptor).
 * As tools da IA rodam na mesma thread da requisição, então também enxergam o usuário certo.
 */
public final class CurrentUser {

    private static final ThreadLocal<Long> ID = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Long userId) {
        ID.set(userId);
    }

    public static void clear() {
        ID.remove();
    }

    /** Id do usuário logado; lança 401 se não houver. */
    public static Long id() {
        Long id = ID.get();
        if (id == null) {
            throw new UnauthorizedException("Faça login para continuar.");
        }
        return id;
    }

    public static Long idOrNull() {
        return ID.get();
    }
}
