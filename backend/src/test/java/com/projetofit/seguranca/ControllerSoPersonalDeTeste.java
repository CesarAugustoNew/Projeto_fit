package com.projetofit.seguranca;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint usado apenas em teste para exercitar a segurança de método ({@code @PreAuthorize}). */
@RestController
class ControllerSoPersonalDeTeste {

    @GetMapping("/api/teste/so-personal")
    @PreAuthorize("hasRole('PERSONAL')")
    String soPersonal() {
        return "ok";
    }
}
