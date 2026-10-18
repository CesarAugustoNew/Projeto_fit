package com.projetofit.usuario;

import com.projetofit.comum.erro.RecursoNaoEncontradoException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UsuarioAtual usuarioAtual;
    private final UsuarioRepository usuarioRepository;

    public MeController(UsuarioAtual usuarioAtual, UsuarioRepository usuarioRepository) {
        this.usuarioAtual = usuarioAtual;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public UsuarioDTO me() {
        return usuarioRepository.findById(usuarioAtual.id())
                .map(UsuarioDTO::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
