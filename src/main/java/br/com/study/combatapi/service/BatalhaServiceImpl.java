package br.com.study.combatapi.service;

import br.com.study.combatapi.feign.GameApiFeign;
import br.com.study.combatapi.model.Batalha;
import br.com.study.combatapi.model.EstadoPersonagemCombate;
import br.com.study.combatapi.model.Inimigo;
import br.com.study.combatapi.model.dto.BatalhaRequest;
import br.com.study.combatapi.model.dto.BatalhaResponse;
import br.com.study.combatapi.model.enums.AcaoTurnoType;
import br.com.study.combatapi.model.enums.ResultadoBatalhaType;
import br.com.study.combatapi.repository.BatalhaRepository;
import br.com.study.genericcrudmongo.service.exception.DataIntegrityException;
import br.com.study.genericcrudmongo.service.exception.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatalhaServiceImpl implements BatalhaService {

    private final BatalhaRepository repositorio;
    private final GameApiFeign gameApiFeign;

    // ─────────────────────────────────────────────
    // Iniciar
    // ─────────────────────────────────────────────

    @Override
    public BatalhaResponse.Inicio iniciar(BatalhaRequest.IniciarBatalha request, Long usuarioId) {
        if (repositorio.existsByPersonagem_PersonagemIdAndResultado(
                request.personagemId(), ResultadoBatalhaType.EM_ANDAMENTO)) {
            throw new DataIntegrityException("Personagem já está em batalha.");
        }

        if (request.hollow()) {
            throw new DataIntegrityException(
                    "Personagem é um Hollow Digital. Sua mente pertence ao sistema."
            );
        }

        Batalha batalha = new Batalha();
        batalha.setUsuarioId(usuarioId);
        batalha.setSessaoId(request.sessaoId());
        batalha.setPersonagem(montarEstadoPersonagem(request));
        batalha.setInimigo(montarInimigo(request));

        repositorio.save(batalha);
        log.info("Batalha iniciada: {} vs {}", request.nomePersonagem(), request.nomeInimigo());

        return new BatalhaResponse.Inicio(
                batalha.getId(), batalha.getSessaoId(),
                batalha.getInimigo().getNome(),
                batalha.getInimigo().getDescricao(),
                batalha.getInimigo().getCategoria(),
                batalha.getInimigo().getHpMaximo(),
                batalha.getInimigo().getAtaque(),
                batalha.getInimigo().getDefesa(),
                batalha.getInimigo().getRecompensaAlmas(),
                batalha.getPersonagem().getHpAtual(),
                batalha.getPersonagem().getMpAtual(),
                batalha.getTurnoAtual()
        );
    }

    // ─────────────────────────────────────────────
    // Executar turno
    // ─────────────────────────────────────────────

    @Override
    public BatalhaResponse.Turno executarTurno(BatalhaRequest.ExecutarTurno request, Long usuarioId) {
        Batalha batalha = buscarBatalhaAtiva(request.batalhaId(), usuarioId);

        if (!batalha.estaEmAndamento()) {
            throw new DataIntegrityException("Esta batalha já foi encerrada.");
        }

        if (request.combo().contains(AcaoTurnoType.USAR_ITEM)) {
            return executarTurnoItem(batalha, request.itemId());
        }

        validarCombo(batalha.getPersonagem(), request.combo());

        // ── Fase do jogador ──────────────────────
        ResultadoCombo resultado = executarCombo(batalha, request.combo());
        batalha.getInimigo().receberDano(resultado.danoTotal());

        // ── Vitória ──────────────────────────────
        if (batalha.getInimigo().estaMorto()) {
            return encerrarComVitoria(batalha, resultado);
        }

        // ── Fase do inimigo ──────────────────────
        int danoRecebido = calcularDanoInimigo(batalha);
        batalha.getPersonagem().receberDano(danoRecebido);

        // ── Derrota ──────────────────────────────
        if (batalha.getPersonagem().estaMorto()) {
            return encerrarComDerrota(batalha, resultado.danoTotal(), danoRecebido);
        }

        batalha.avancarTurno();
        repositorio.save(batalha);

        return new BatalhaResponse.Turno(
                resultado.danoTotal(), danoRecebido, resultado.critico(),
                resultado.memoriaQueimada(),
                batalha.getPersonagem().getHpAtual(),
                batalha.getPersonagem().getMpAtual(),
                batalha.getInimigo().getHpAtual(),
                batalha.getTurnoAtual(),
                ResultadoBatalhaType.EM_ANDAMENTO,
                false, null, null, null
        );
    }

    @Override
    public BatalhaResponse.Estado buscarEstado(String batalhaId, Long usuarioId) {
        return toEstado(buscarBatalhaAtiva(batalhaId, usuarioId));
    }

    // ─────────────────────────────────────────────
    // Execução do combo
    // ─────────────────────────────────────────────

    private ResultadoCombo executarCombo(Batalha batalha, List<AcaoTurnoType> combo) {
        EstadoPersonagemCombate p = batalha.getPersonagem();
        Inimigo inimigo = batalha.getInimigo();
        int danoTotal = 0;
        boolean critico = false;
        String memoriaQueimada = null;

        for (AcaoTurnoType acao : combo) {
            switch (acao) {
                case ATACAR -> {
                    var r = calcularDanoAtaque(p, inimigo, 1.0);
                    danoTotal += r.dano();
                    if (r.critico() && memoriaQueimada == null) {
                        critico = true;
                        memoriaQueimada = queimarMemoriaDoPersonagem(p);
                    }
                }
                case ATAQUE_FORTE -> {
                    var r = calcularDanoAtaque(p, inimigo, 1.8);
                    danoTotal += r.dano();
                    if (r.critico() && memoriaQueimada == null) {
                        critico = true;
                        memoriaQueimada = queimarMemoriaDoPersonagem(p);
                    }
                }
                case ESQUIVAR -> p.setEsquivaAtiva(true);
                case DEFENDER -> p.setDefesaAtiva(true);
                case ESPECIAL -> {
                    danoTotal += (int) (p.getAtaque() * 2.0);
                    p.gastarMp(3);
                }
                default -> {
                }
            }
        }

        return new ResultadoCombo(danoTotal, critico, memoriaQueimada);
    }

    /**
     * Tenta queimar uma memória via Feign na game-api.
     * Se não houver memórias disponíveis, o crítico ocorre sem custo narrativo.
     */
    private String queimarMemoriaDoPersonagem(EstadoPersonagemCombate p) {
        if (!p.isTemMemoriasDisponiveis()) return null;
        try {
            var response = gameApiFeign.queimarMemoria(p.getPersonagemId()).getBody();
            if (response == null) return null;
            // Atualiza flag local
            p.setTemMemoriasDisponiveis(false);
            return response.descricao();
        } catch (Exception e) {
            log.warn("Não foi possível queimar memória — crítico sem custo narrativo");
            return null;
        }
    }

    private AtaqueDano calcularDanoAtaque(EstadoPersonagemCombate p, Inimigo inimigo, double mult) {
        int danoBase = (int) (p.getAtaque() * mult);
        int danoReduzido = Math.max(1, danoBase - inimigo.getDefesa());
        boolean critico = p.isTemMemoriasDisponiveis()
                && Math.random() < (p.getSorte() / 100.0);
        int dano = critico ? danoReduzido * 2 : danoReduzido + variacao(danoReduzido);
        return new AtaqueDano(dano, critico);
    }

    private int calcularDanoInimigo(Batalha b) {
        int base = b.getInimigo().getAtaque();
        return base + variacao(base);
    }

    private int variacao(int base) {
        return (int) (base * (Math.random() * 0.2 - 0.1));
    }

    // ─────────────────────────────────────────────
    // Turno com item
    // ─────────────────────────────────────────────

    private BatalhaResponse.Turno executarTurnoItem(Batalha batalha, Long itemId) {
        if (itemId == null) throw new DataIntegrityException("itemId é obrigatório.");
        int danoInimigo = (int) (batalha.getInimigo().getAtaque() * 1.2);
        batalha.getPersonagem().receberDano(danoInimigo);
        if (batalha.getPersonagem().estaMorto()) {
            return encerrarComDerrota(batalha, 0, danoInimigo);
        }
        batalha.avancarTurno();
        repositorio.save(batalha);
        return new BatalhaResponse.Turno(
                0, danoInimigo, false, null,
                batalha.getPersonagem().getHpAtual(),
                batalha.getPersonagem().getMpAtual(),
                batalha.getInimigo().getHpAtual(),
                batalha.getTurnoAtual(),
                ResultadoBatalhaType.EM_ANDAMENTO,
                false, null, null, null
        );
    }

    // ─────────────────────────────────────────────
    // Fim de batalha
    // ─────────────────────────────────────────────

    private BatalhaResponse.Turno encerrarComVitoria(Batalha batalha, ResultadoCombo resultado) {
        long bitsGanhos = batalha.getInimigo().getRecompensaAlmas();
        batalha.setResultado(ResultadoBatalhaType.VITORIA);
        repositorio.save(batalha);

        gameApiFeign.sincronizarVitoria(
                batalha.getPersonagem().getPersonagemId(),
                new GameApiFeign.SincronizarVitoriaRequest(
                        batalha.getPersonagem().getHpAtual(),
                        batalha.getPersonagem().getMpAtual(),
                        bitsGanhos, bitsGanhos / 10
                )
        );

        repositorio.deleteById(batalha.getId());
        return new BatalhaResponse.Turno(
                resultado.danoTotal(), 0, resultado.critico(), resultado.memoriaQueimada(),
                batalha.getPersonagem().getHpAtual(),
                batalha.getPersonagem().getMpAtual(),
                0, batalha.getTurnoAtual(),
                ResultadoBatalhaType.VITORIA,
                true, null, null, bitsGanhos
        );
    }

    private BatalhaResponse.Turno encerrarComDerrota(Batalha batalha, int danoCausado, int danoRecebido) {
        EstadoPersonagemCombate p = batalha.getPersonagem();
        batalha.setResultado(ResultadoBatalhaType.DERROTA);
        repositorio.save(batalha);

        Long soulDropId = null;
        String localizacao = "Servidor %s — Turno %d".formatted(
                batalha.getInimigo().getNome(), batalha.getTurnoAtual());

        if (p.temBits()) {
            // Morte normal — cria SoulDrop com os Bits
            var drop = gameApiFeign.notificarMorte(p.getPersonagemId(),
                    new GameApiFeign.MorteRequest(p.getBitsConsciencia(), localizacao)).getBody();
            if (drop != null) soulDropId = drop.soulDropId();
        } else {
            // Hollow Digital — sem Bits, mente vira NPC
            gameApiFeign.notificarHollow(p.getPersonagemId());
            log.info("HOLLOW DIGITAL: {} foi corrompido pelo sistema", p.getNome());
        }

        repositorio.deleteById(batalha.getId());
        return new BatalhaResponse.Turno(
                danoCausado, danoRecebido, false, null,
                0, p.getMpAtual(),
                batalha.getInimigo().getHpAtual(),
                batalha.getTurnoAtual(),
                p.temBits() ? ResultadoBatalhaType.DERROTA : ResultadoBatalhaType.DERROTA,
                true, soulDropId, localizacao, null
        );
    }

    // ─────────────────────────────────────────────
    // Validações e helpers
    // ─────────────────────────────────────────────

    private void validarCombo(EstadoPersonagemCombate p, List<AcaoTurnoType> combo) {
        int custoAp = combo.stream().mapToInt(AcaoTurnoType::getCustAp).sum();
        if (custoAp > p.getApMaximo()) {
            throw new DataIntegrityException(
                    "AP insuficiente. Custo: %d, disponível: %d".formatted(custoAp, p.getApMaximo())
            );
        }
        long qtdEspecial = combo.stream().filter(a -> a == AcaoTurnoType.ESPECIAL).count();
        if (qtdEspecial > 0 && p.getMpAtual() < 3 * qtdEspecial) {
            throw new DataIntegrityException(
                    "MP insuficiente. Necessário: %d, disponível: %d"
                            .formatted(3 * qtdEspecial, p.getMpAtual())
            );
        }
    }

    private EstadoPersonagemCombate montarEstadoPersonagem(BatalhaRequest.IniciarBatalha r) {
        EstadoPersonagemCombate p = new EstadoPersonagemCombate();
        p.setPersonagemId(r.personagemId());
        p.setNome(r.nomePersonagem());
        p.setHpAtual(r.hpAtual());
        p.setHpMaximo(r.hpMaximo());
        p.setMpAtual(r.mpAtual());
        p.setMpMaximo(r.mpMaximo());
        p.setApMaximo(r.apMaximo());
        p.setAtaque(r.ataque());
        p.setDefesa(r.defesa());
        p.setVelocidade(r.velocidade());
        p.setSorte(r.sorte());
        p.setBitsConsciencia(r.bitsConsciencia());
        p.setTemMemoriasDisponiveis(r.temMemoriasDisponiveis());
        return p;
    }

    private Inimigo montarInimigo(BatalhaRequest.IniciarBatalha r) {
        double mult = r.categoriaInimigo().getMultiplicadorAtributos();
        Inimigo inimigo = new Inimigo();
        inimigo.setNome(r.nomeInimigo());
        inimigo.setDescricao(r.descricaoInimigo());
        inimigo.setCategoria(r.categoriaInimigo());
        inimigo.setHpMaximo((int) (r.hpInimigo() * mult));
        inimigo.setHpAtual((int) (r.hpInimigo() * mult));
        inimigo.setAtaque((int) (r.ataqueInimigo() * mult));
        inimigo.setDefesa(r.defesaInimigo());
        inimigo.setRecompensaAlmas((long) (r.recompensaAlmas() * r.categoriaInimigo().getMultiplicadorAlmas()));
        return inimigo;
    }

    private Batalha buscarBatalhaAtiva(String batalhaId, Long usuarioId) {
        return repositorio.findByIdAndUsuarioId(batalhaId, usuarioId)
                .orElseThrow(() -> new ObjectNotFoundException("Batalha não encontrada: " + batalhaId));
    }

    private BatalhaResponse.Estado toEstado(Batalha b) {
        return new BatalhaResponse.Estado(
                b.getId(), b.getTurnoAtual(), b.getResultado(),
                b.getPersonagem().getHpAtual(), b.getPersonagem().getHpMaximo(),
                b.getPersonagem().getMpAtual(), b.getPersonagem().getMpMaximo(),
                b.getInimigo().getNome(), b.getInimigo().getCategoria(),
                b.getInimigo().getHpAtual(), b.getInimigo().getHpMaximo()
        );
    }

    // Records internos
    private record ResultadoCombo(int danoTotal, boolean critico, String memoriaQueimada) {
    }

    private record AtaqueDano(int dano, boolean critico) {
    }
}