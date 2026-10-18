package com.projetofit.usuario;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o usuário {@link Papel#PERSONAL} no primeiro acesso e avisa os {@link AoCriarPersonal} na mesma transação:
 * qualquer exceção (inclusive a violação de unicidade de uma inserção concorrente) desfaz tudo.
 */
@Service
public class BootstrapPersonal {

    private final UsuarioRepository usuarioRepository;
    private final List<AoCriarPersonal> ouvintes;

    public BootstrapPersonal(UsuarioRepository usuarioRepository, List<AoCriarPersonal> ouvintes) {
        this.usuarioRepository = usuarioRepository;
        this.ouvintes = ouvintes;
    }

    @Transactional
    public Usuario criar(UUID id, String nome, String email) {
        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(id, nome, email, Papel.PERSONAL));
        for (AoCriarPersonal ouvinte : ouvintes) {
            ouvinte.aoCriarPersonal(id);
        }
        return usuario;
    }
}
