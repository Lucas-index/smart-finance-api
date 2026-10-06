package com.smartapi.service;

import java.util.Locale;

final class CategoryNormalizer {

    private CategoryNormalizer() {
    }

    static String normalize(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Categoria é obrigatória.");
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }
}
