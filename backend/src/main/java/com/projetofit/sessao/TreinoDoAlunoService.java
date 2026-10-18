package com.projetofit.sessao;

import com.projetofit.aluno.Aluno;
import com.projetofit.ficha.Divisao;
import com.projetofit.ficha.Ficha;
import com.projetofit.ficha.FichaDTO;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.ficha.StatusFicha;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreinoDoAlunoService {

    private final AlunoLogado alunoLogado;
    private final FichaRepository fichaRepository;
    private final SessaoRepository sessaoRepository;
    private final Clock clock;

    public TreinoDoAlunoService(AlunoLogado alunoLogado, FichaRepository fichaRepository,
                                SessaoRepository sessaoRepository, Clock clock) {
        this.alunoLogado = alunoLogado;
        this.fichaRepository = fichaRepository;
        this.sessaoRepository = sessaoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<FichaAtivaDTO> fichaAtiva() {
        Aluno aluno = alunoLogado.obter();
        return fichaRepository.findByAlunoIdAndStatus(aluno.getId(), StatusFicha.ATIVA)
                .map(ficha -> new FichaAtivaDTO(
                        FichaDTO.de(ficha, LocalDate.now(clock), clock.getZone()),
                        proximaDivisaoId(aluno, ficha)));
    }

    private UUID proximaDivisaoId(Aluno aluno, Ficha ficha) {
        List<Divisao> divisoes = ficha.getDivisoes().stream()
                .sorted(Comparator.comparingInt(Divisao::getOrdem)).toList();
        if (divisoes.isEmpty()) {
            return null;
        }
        Optional<Sessao> ultima = sessaoRepository
                .findFirstByAlunoIdAndDivisaoFichaIdOrderByFinalizadaEmDesc(aluno.getId(), ficha.getId());
        if (ultima.isEmpty()) {
            return divisoes.get(0).getId();
        }
        UUID ultimaId = ultima.get().getDivisao().getId();
        for (int i = 0; i < divisoes.size(); i++) {
            if (divisoes.get(i).getId().equals(ultimaId)) {
                return divisoes.get((i + 1) % divisoes.size()).getId();
            }
        }
        return divisoes.get(0).getId();
    }
}
