package com.projetofit.comum;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurações da aplicação (prefixo {@code app}). */
@ConfigurationProperties("app")
public record AppProperties(String supabaseUrl, String personalEmail, String frontUrl,
        String supabaseServiceRoleKey) {
}
