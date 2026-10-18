package com.projetofit.comum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppPropertiesTest {

    @Test
    void toStringNaoExpoeAChave() {
        AppProperties props = new AppProperties("https://x.supabase.co", "personal@teste.com",
                "http://localhost:4200", "chave-secreta-service-role");

        assertThat(props.toString())
                .doesNotContain("chave-secreta-service-role")
                .contains("supabaseServiceRoleKey=***")
                .contains("https://x.supabase.co");
    }
}
