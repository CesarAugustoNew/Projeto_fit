package com.projetofit.comum;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio da aplicação: "hoje" segue o fuso do personal ({@code America/Sao_Paulo}), não o do servidor. */
@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
