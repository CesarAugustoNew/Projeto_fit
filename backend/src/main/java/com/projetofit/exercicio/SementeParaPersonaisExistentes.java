package com.projetofit.exercicio;

import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Na inicialização, semeia o catálogo dos personais que já existiam e ainda não têm exercícios. */
@Component
public class SementeParaPersonaisExistentes implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SementeParaPersonaisExistentes.class);

    private final UsuarioRepository usuarioRepository;
    private final SementeExercicios sementeExercicios;

    public SementeParaPersonaisExistentes(UsuarioRepository usuarioRepository, SementeExercicios sementeExercicios) {
        this.usuarioRepository = usuarioRepository;
        this.sementeExercicios = sementeExercicios;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Usuario personal : usuarioRepository.findByPapel(Papel.PERSONAL)) {
            try {
                sementeExercicios.semearSeVazio(personal.getId());
            } catch (RuntimeException e) {
                log.error("Falha ao semear exercícios do personal {}", personal.getId(), e);
            }
        }
    }
}
