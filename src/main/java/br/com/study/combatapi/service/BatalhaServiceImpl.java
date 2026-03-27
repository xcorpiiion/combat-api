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
    // Iniciar batalha
    // ─────────────────────────────────────────────

    @Override
    public BatalhaResponse.Inicio iniciar(BatalhaRequest.IniciarBatalha request, Long usuarioId) {
        if (repositorio.existsByPersonagem_PersonagemIdAndResultado(
                request.personagemId(), ResultadoBatalhaType.EM_ANDAMENTO)) {
            throw new DataIntegrityException(
                    "Personagem já está em batalha. Termine o combate atual antes de iniciar outro."
            );
        }

        Batalha batalha = new Batalha();
        batalha.setUsuarioId(usuarioId);
        batalha.setSessaoId(request.sessaoId());
        batalha.setPersonagem(montarEstadoPersonagem(request));
        batalha.setInimigo(montarInimigo(request));

        repositorio.save(batalha);
        log.info("Batalha iniciada: {} vs {} (id: {})",
                request.nomePersonagem(), request.nomeInimigo(), batalha.getId());

        return new BatalhaResponse.Inicio(
                batalha.getId(),
                batalha.getSessaoId(),
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

        // USAR_ITEM descarta o combo — inimigo age sem oposição
        if (request.combo().contains(AcaoTurnoType.USAR_ITEM)) {
            return executarTurnoItem(batalha, request.itemId());
        }

        validarCombo(batalha.getPersonagem(), request.combo());

        // ── Fase do jogador ──────────────────────
        int danoCausado = executarCombo(batalha, request.combo());
        batalha.getInimigo().receberDano(danoCausado);

        // ── Verifica vitória ─────────────────────
        if (batalha.getInimigo().estaMorto()) {
            return encerrarComVitoria(batalha, danoCausado);
        }

        // ── Fase do inimigo ──────────────────────
        int danoRecebido = calcularDanoInimigo(batalha);
        batalha.getPersonagem().receberDano(danoRecebido);

        // ── Verifica derrota ─────────────────────
        if (batalha.getPersonagem().estaMorto()) {
            return encerrarComDerrota(batalha, danoCausado, danoRecebido);
        }

        // ── Avança o turno ───────────────────────
        batalha.avancarTurno();
        repositorio.save(batalha);

        log.info("Turno {} concluído — dano causado: {}, dano recebido: {}",
                batalha.getTurnoAtual(), danoCausado, danoRecebido);

        return new BatalhaResponse.Turno(
                danoCausado, danoRecebido, false,
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
        Batalha batalha = buscarBatalhaAtiva(batalhaId, usuarioId);
        return toEstado(batalha);
    }

    // ─────────────────────────────────────────────
    // Execução do combo
    // ─────────────────────────────────────────────

    private int executarCombo(Batalha batalha, List<AcaoTurnoType> combo) {
        EstadoPersonagemCombate p = batalha.getPersonagem();
        Inimigo inimigo = batalha.getInimigo();
        int danoTotal = 0;

        for (AcaoTurnoType acao : combo) {
            switch (acao) {
                case ATACAR -> danoTotal += calcularDanoAtaque(p, inimigo, 1.0);
                case ATAQUE_FORTE -> danoTotal += calcularDanoAtaque(p, inimigo, 1.8);
                case ESQUIVAR -> p.setEsquivaAtiva(true);
                case DEFENDER -> p.setDefesaAtiva(true);
                case ESPECIAL -> {
                    danoTotal += calcularEspecial(p, inimigo);
                    p.gastarMp(3);
                }
                default -> { /* USAR_ITEM já tratado antes */ }
            }
        }
        return danoTotal;
    }

    private int calcularDanoAtaque(EstadoPersonagemCombate p, Inimigo inimigo, double multiplicador) {
        int danoBase = (int) (p.getAtaque() * multiplicador);
        int danoReduzido = Math.max(1, danoBase - inimigo.getDefesa());
        boolean critico = Math.random() < (p.getSorte() / 100.0);
        return critico ? danoReduzido * 2 : danoReduzido + variacao(danoReduzido);
    }

    private int calcularEspecial(EstadoPersonagemCombate p, Inimigo inimigo) {
        // Especial ignora DEF do inimigo — dano mágico puro
        int danoBase = (int) (p.getAtaque() * 2.0);
        return danoBase + variacao(danoBase);
    }

    private int calcularDanoInimigo(Batalha batalha) {
        int base = batalha.getInimigo().getAtaque();
        return base + variacao(base);
    }

    private int variacao(int base) {
        return (int) (base * (Math.random() * 0.2 - 0.1)); // ±10%
    }

    // ─────────────────────────────────────────────
    // Turno com item
    // ─────────────────────────────────────────────

    private BatalhaResponse.Turno executarTurnoItem(Batalha batalha, Long itemId) {
        if (itemId == null) {
            throw new DataIntegrityException("itemId é obrigatório ao usar USAR_ITEM.");
        }

        // Inimigo aproveita a abertura com 20% de bônus de dano
        int danoInimigo = (int) (batalha.getInimigo().getAtaque() * 1.2);
        batalha.getPersonagem().receberDano(danoInimigo);

        if (batalha.getPersonagem().estaMorto()) {
            return encerrarComDerrota(batalha, 0, danoInimigo);
        }

        batalha.avancarTurno();
        repositorio.save(batalha);

        return new BatalhaResponse.Turno(
                0, danoInimigo, false,
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

    private BatalhaResponse.Turno encerrarComVitoria(Batalha batalha, int danoCausado) {
        long almasGanhas = batalha.getInimigo().getRecompensaAlmas();
        long xpGanha = almasGanhas / 10;

        batalha.setResultado(ResultadoBatalhaType.VITORIA);
        repositorio.save(batalha);

        // Sincroniza com a game-api
        gameApiFeign.sincronizarVitoria(
                batalha.getPersonagem().getPersonagemId(),
                new GameApiFeign.SincronizarVitoriaRequest(
                        batalha.getPersonagem().getHpAtual(),
                        batalha.getPersonagem().getMpAtual(),
                        almasGanhas,
                        xpGanha
                )
        );

        repositorio.deleteById(batalha.getId());
        log.info("Vitória! {} derrotou {} — {} almas ganhas",
                batalha.getPersonagem().getNome(),
                batalha.getInimigo().getNome(),
                almasGanhas);

        return new BatalhaResponse.Turno(
                danoCausado, 0, false,
                batalha.getPersonagem().getHpAtual(),
                batalha.getPersonagem().getMpAtual(),
                0, batalha.getTurnoAtual(),
                ResultadoBatalhaType.VITORIA,
                true, null, null, almasGanhas
        );
    }

    private BatalhaResponse.Turno encerrarComDerrota(Batalha batalha, int danoCausado, int danoRecebido) {
        batalha.setResultado(ResultadoBatalhaType.DERROTA);
        repositorio.save(batalha);

        // Notifica a game-api — ela cria o SoulDrop
        String localizacao = "Derrotado por %s no turno %d"
                .formatted(batalha.getInimigo().getNome(), batalha.getTurnoAtual());

        var soulDrop = gameApiFeign.notificarMorte(
                batalha.getPersonagem().getPersonagemId(),
                new GameApiFeign.MorteRequest(
                        batalha.getPersonagem().getHpAtual(), // almas — a game-api sabe o valor real
                        localizacao
                )
        ).getBody();

        repositorio.deleteById(batalha.getId());
        log.info("Derrota! {} foi derrotado por {}",
                batalha.getPersonagem().getNome(),
                batalha.getInimigo().getNome());

        return new BatalhaResponse.Turno(
                danoCausado, danoRecebido, false,
                0, batalha.getPersonagem().getMpAtual(),
                batalha.getInimigo().getHpAtual(),
                batalha.getTurnoAtual(),
                ResultadoBatalhaType.DERROTA,
                true,
                soulDrop != null ? soulDrop.soulDropId() : null,
                soulDrop != null ? soulDrop.localizacao() : localizacao,
                null
        );
    }

    // ─────────────────────────────────────────────
    // Validações
    // ─────────────────────────────────────────────

    private void validarCombo(EstadoPersonagemCombate p, List<AcaoTurnoType> combo) {
        int custoAp = combo.stream()
                .mapToInt(AcaoTurnoType::getCustAp)
                .sum();

        if (custoAp > p.getApMaximo()) {
            throw new DataIntegrityException(
                    "AP insuficiente. Custo: %d, disponível: %d".formatted(custoAp, p.getApMaximo())
            );
        }

        long qtdEspecial = combo.stream().filter(a -> a == AcaoTurnoType.ESPECIAL).count();
        if (qtdEspecial > 0 && p.getMpAtual() < 3 * qtdEspecial) {
            throw new DataIntegrityException(
                    "MP insuficiente para ESPECIAL. Necessário: %d, disponível: %d"
                            .formatted(3 * qtdEspecial, p.getMpAtual())
            );
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

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
                b.getId(),
                b.getTurnoAtual(),
                b.getResultado(),
                b.getPersonagem().getHpAtual(),
                b.getPersonagem().getHpMaximo(),
                b.getPersonagem().getMpAtual(),
                b.getPersonagem().getMpMaximo(),
                b.getInimigo().getNome(),
                b.getInimigo().getCategoria(),
                b.getInimigo().getHpAtual(),
                b.getInimigo().getHpMaximo()
        );
    }
}