package com.projetofit.comum;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurações da aplicação (prefixo {@code app}). */
@ConfigurationProperties("app")
public record AppProperties(String supabaseUrl, String personalEmail, String frontUrl,
        String supabaseServiceRoleKey) {

    /** Mascara a service-role key para que ela nunca apareça em logs. */
    @Override
    public String toString() {
        String chave = supabaseServiceRoleKey == null || supabaseServiceRoleKey.isEmpty() ? "" : "***";
        return "AppProperties[supabaseUrl=" + supabaseUrl + ", personalEmail=" + personalEmail
                + ", frontUrl=" + frontUrl + ", supabaseServiceRoleKey=" + chave + "]";
    }
}
