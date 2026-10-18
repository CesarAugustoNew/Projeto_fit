package com.projetofit.usuario;

import com.projetofit.aluno.AlunoAcesso;
import com.projetofit.comum.AppProperties;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Descobre o {@link Usuario} dono de um token do Supabase.
 *
 * <p>Busca por {@code id} (= {@code sub}). No primeiro acesso do personal (e-mail igual a
 * {@code app.personal-email}, ignorando caixa) cria o usuário {@link Papel#PERSONAL}.
 *
 * <p>Usuário {@link Papel#ALUNO} passa por {@link AlunoAcesso#liberar}: o primeiro acesso de um aluno
 * {@code CONVIDADO} o torna {@code ATIVO}; aluno {@code INATIVO} (ou sem cadastro de aluno) fica sem papel e recebe
 * 403.
 *
 * <p>Ponto de extensão: a Etapa 3 acrescenta a semente de exercícios no bootstrap.
 *
 * <p>Sem {@code @Transactional} de propósito: cada chamada ao repositório (e a {@link AlunoAcesso#liberar}) tem a
 * própria transação, então a violação de unicidade de uma inserção concorrente não marca uma transação externa
 * para rollback e a releitura funciona.
 */
@Component
public class UsuarioAtualResolver {

    private static final Logger log = LoggerFactory.getLogger(UsuarioAtualResolver.class);

    private final UsuarioRepository usuarioRepository;
    private final AppProperties appProperties;
    private final AlunoAcesso alunoAcesso;

    public UsuarioAtualResolver(UsuarioRepository usuarioRepository, AppProperties appProperties,
                                AlunoAcesso alunoAcesso) {
        this.usuarioRepository = usuarioRepository;
        this.appProperties = appProperties;
        this.alunoAcesso = alunoAcesso;
    }

    public Optional<Usuario> resolver(UUID sub, String email) {
        Optional<Usuario> existente = usuarioRepository.findById(sub);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (usuario.getPapel() == Papel.ALUNO && !alunoAcesso.liberar(usuario.getId())) {
                return Optional.empty();
            }
            return existente;
        }
        if (!ehEmailDoPersonal(email)) {
            return Optional.empty();
        }
        String emailNormalizado = email.toLowerCase(Locale.ROOT);
        String nome = emailNormalizado.substring(0, emailNormalizado.indexOf('@'));
        try {
            return Optional.of(usuarioRepository.saveAndFlush(new Usuario(sub, nome, emailNormalizado, Papel.PERSONAL)));
        } catch (DataIntegrityViolationException e) {
            // Outra requisição criou o usuário ao mesmo tempo (ou o e-mail já pertence a outro id).
            Optional<Usuario> relido = usuarioRepository.findById(sub);
            if (relido.isEmpty()) {
                log.warn("Bootstrap do personal recusado: e-mail já cadastrado para outro id (sub {})", sub);
            }
            return relido;
        }
    }

    private boolean ehEmailDoPersonal(String email) {
        String personalEmail = appProperties.personalEmail();
        return email != null && personalEmail != null && !personalEmail.isBlank()
                && email.indexOf('@') > 0
                && email.equalsIgnoreCase(personalEmail.trim());
    }
}
