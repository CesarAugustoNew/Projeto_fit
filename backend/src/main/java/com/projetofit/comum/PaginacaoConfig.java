package com.projetofit.comum;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Serializa {@code Page} como {@code {content, page:{size, number, totalElements, totalPages}}} e limita o
 * tamanho da página. Habilitar o suporte explicitamente desliga a auto-configuração do Boot, então
 * {@code spring.data.web.pageable.*} não vale aqui: os limites ficam neste bean.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class PaginacaoConfig {

    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 50;

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer limitesDePagina() {
        return resolver -> {
            resolver.setMaxPageSize(TAMANHO_MAXIMO);
            resolver.setFallbackPageable(PageRequest.of(0, TAMANHO_PADRAO));
        };
    }
}
