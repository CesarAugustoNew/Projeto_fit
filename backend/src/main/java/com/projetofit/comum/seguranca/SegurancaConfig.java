package com.projetofit.comum.seguranca;

import com.projetofit.comum.AppProperties;
import jakarta.servlet.DispatcherType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * API stateless protegida por JWT do Supabase. O papel vem da tabela {@code usuario}
 * (ver {@link UsuarioJwtAuthenticationConverter}).
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SegurancaConfig {

    static final String ORIGEM_LOCAL = "http://localhost:4200";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioJwtAuthenticationConverter conversor,
            RespostasDeSegurancaProblem respostas) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/**").hasAnyRole("PERSONAL", "ALUNO")
                        // Forward do container para /error (exceção que escapou dos filtros, ex.: banco fora do ar
                        // durante a conversão do JWT): deixa sair o 5xx original em vez de virar 401/403.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas))
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.copyOf(new LinkedHashSet<>(
                Stream.of(appProperties.frontUrl(), ORIGEM_LOCAL).filter(Objects::nonNull).toList())));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", cors);
        return fonte;
    }
}
