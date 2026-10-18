package com.projetofit.usuario;

import java.util.UUID;

public record UsuarioDTO(UUID id, String nome, String email, Papel papel) {

    static UsuarioDTO de(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel());
    }
}
