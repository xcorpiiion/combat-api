package br.com.study.combatapi.service;

import br.com.study.combatapi.feign.GameApiFeign;
import br.com.study.combatapi.model.Batalha;
import br.com.study.combatapi.model.EstadoPersonagemCombate;
import br.com.study.combatapi.model.Inimigo;
import br.com.study.combatapi.model.dto.BatalhaRequest;
import br.com.study.combatapi.model.dto.BatalhaResponse;
import br.com.study.combatapi.model.enums.AcaoTurnoType;
import br.com.study.combatapi.model.enums.CategoriaInimigoType;
import br.com.study.combatapi.model.enums.ResultadoBatalhaType;
import br.com.study.combatapi.repository.BatalhaRepository;
import br.com.study.genericcrudmongo.service.exception.DataIntegrityException;
import br.com.study.genericcrudmongo.service.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatalhaServiceImpl")
class BatalhaServiceImplTest {

    @Mock
    BatalhaRepository repositorio;
    @Mock
    GameApiFeign gameApiFeign;
    @InjectMocks
    BatalhaServiceImpl service;

    private static final Long USUARIO_ID = 1L;
    private static final Long PERSONAGEM_ID = 1L;
    private static final String BATALHA_ID = "batalha-uuid-123";

    private BatalhaRequest.IniciarBatalha requestIniciar;
    private Batalha batalhaEmAndamento;

    @BeforeEach
    void setUp() {
        requestIniciar = new BatalhaRequest.IniciarBatalha(
                PERSONAGEM_ID, "sessao-123",
                "Solaire",
                100, 150, 40, 50, 4,
                18, 12, 8, 5,
                "Cavaleiro Oco", "Um guerreiro sem alma",
                CategoriaInimigoType.NORMAL,
                80, 12, 5, 100L
        );

        EstadoPersonagemCombate personagem = new EstadoPersonagemCombate();
        personagem.setPersonagemId(PERSONAGEM_ID);
        personagem.setNome("Solaire");
        personagem.setHpAtual(100);
        personagem.setHpMaximo(150);
        personagem.setMpAtual(40);
        personagem.setMpMaximo(50);
        personagem.setApMaximo(4);
        personagem.setAtaque(18);
        personagem.setDefesa(12);
        personagem.setVelocidade(8);
        personagem.setSorte(5);

        Inimigo inimigo = new Inimigo();
        inimigo.setNome("Cavaleiro Oco");
        inimigo.setCategoria(CategoriaInimigoType.NORMAL);
        inimigo.setHpMaximo(80);
        inimigo.setHpAtual(80);
        inimigo.setAtaque(12);
        inimigo.setDefesa(5);
        inimigo.setRecompensaAlmas(100L);

        batalhaEmAndamento = new Batalha();
        batalhaEmAndamento.setId(BATALHA_ID);
        batalhaEmAndamento.setUsuarioId(USUARIO_ID);
        batalhaEmAndamento.setSessaoId("sessao-123");
        batalhaEmAndamento.setPersonagem(personagem);
        batalhaEmAndamento.setInimigo(inimigo);
        batalhaEmAndamento.setResultado(ResultadoBatalhaType.EM_ANDAMENTO);
    }

    // ─────────────────────────────────────────────
    // iniciar
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("iniciar")
    class Iniciar {

        @Test
        @DisplayName("inicia batalha com sucesso")
        void requestValido_iniciaBatalha() {
            when(repositorio.existsByPersonagem_PersonagemIdAndResultado(
                    PERSONAGEM_ID, ResultadoBatalhaType.EM_ANDAMENTO)).thenReturn(false);
            when(repositorio.save(any())).thenAnswer(inv -> {
                Batalha b = inv.getArgument(0);
                b.setId(BATALHA_ID);
                return b;
            });

            BatalhaResponse.Inicio response = service.iniciar(requestIniciar, USUARIO_ID);

            assertThat(response.batalhaId()).isEqualTo(BATALHA_ID);
            assertThat(response.nomeInimigo()).isEqualTo("Cavaleiro Oco");
            assertThat(response.turnoAtual()).isEqualTo(1);
            verify(repositorio).save(any());
        }

        @Test
        @DisplayName("aplica multiplicador de categoria no inimigo Elite")
        void categoriaElite_aplicaMultiplicador() {
            var requestElite = new BatalhaRequest.IniciarBatalha(
                    PERSONAGEM_ID, "sessao-123",
                    "Solaire",
                    100, 150, 40, 50, 4,
                    18, 12, 8, 5,
                    "Cavaleiro Elite", "Versão poderosa",
                    CategoriaInimigoType.ELITE,
                    80, 12, 5, 100L
            );

            when(repositorio.existsByPersonagem_PersonagemIdAndResultado(any(), any())).thenReturn(false);
            when(repositorio.save(any())).thenAnswer(inv -> {
                Batalha b = inv.getArgument(0);
                b.setId(BATALHA_ID);
                return b;
            });

            BatalhaResponse.Inicio response = service.iniciar(requestElite, USUARIO_ID);

            // Elite tem multiplicador 1.5 — HP base 80 * 1.5 = 120
            assertThat(response.hpInimigo()).isEqualTo(120);
        }

        @Test
        @DisplayName("lança DataIntegrityException quando já existe batalha em andamento")
        void batalhaAtiva_lancaExcecao() {
            when(repositorio.existsByPersonagem_PersonagemIdAndResultado(
                    PERSONAGEM_ID, ResultadoBatalhaType.EM_ANDAMENTO)).thenReturn(true);

            assertThatThrownBy(() -> service.iniciar(requestIniciar, USUARIO_ID))
                    .isInstanceOf(DataIntegrityException.class)
                    .hasMessageContaining("batalha");

            verify(repositorio, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // executarTurno
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("executarTurno")
    class ExecutarTurno {

        @Test
        @DisplayName("combo válido executa e retorna turno em andamento")
        void comboValido_retornaTurno() {
            var request = new BatalhaRequest.ExecutarTurno(
                    BATALHA_ID, List.of(AcaoTurnoType.ATACAR), null
            );
            when(repositorio.findByIdAndUsuarioId(BATALHA_ID, USUARIO_ID))
                    .thenReturn(Optional.of(batalhaEmAndamento));
            when(repositorio.save(any())).thenReturn(batalhaEmAndamento);

            BatalhaResponse.Turno response = service.executarTurno(request, USUARIO_ID);

            assertThat(response.batalhaEncerrada()).isFalse();
            assertThat(response.resultado()).isEqualTo(ResultadoBatalhaType.EM_ANDAMENTO);
            assertThat(response.danoCausado()).isPositive();
        }

        @Test
        @DisplayName("lança DataIntegrityException quando AP é insuficiente")
        void apInsuficiente_lancaExcecao() {
            // AP máximo é 4, combo custa 5 (ATAQUE_FORTE=2 + ATAQUE_FORTE=2 + ATACAR=1)
            var request = new BatalhaRequest.ExecutarTurno(
                    BATALHA_ID,
                    List.of(AcaoTurnoType.ATAQUE_FORTE, AcaoTurnoType.ATAQUE_FORTE, AcaoTurnoType.ATACAR),
                    null
            );
            when(repositorio.findByIdAndUsuarioId(BATALHA_ID, USUARIO_ID))
                    .thenReturn(Optional.of(batalhaEmAndamento));

            assertThatThrownBy(() -> service.executarTurno(request, USUARIO_ID))
                    .isInstanceOf(DataIntegrityException.class)
                    .hasMessageContaining("AP");
        }

        @Test
        @DisplayName("lança DataIntegrityException quando MP é insuficiente para ESPECIAL")
        void mpInsuficiente_lancaExcecao() {
            batalhaEmAndamento.getPersonagem().setMpAtual(2); // menos que 3

            var request = new BatalhaRequest.ExecutarTurno(
                    BATALHA_ID, List.of(AcaoTurnoType.ESPECIAL), null
            );
            when(repositorio.findByIdAndUsuarioId(BATALHA_ID, USUARIO_ID))
                    .thenReturn(Optional.of(batalhaEmAndamento));

            assertThatThrownBy(() -> service.executarTurno(request, USUARIO_ID))
                    .isInstanceOf(DataIntegrityException.class)
                    .hasMessageContaining("MP");
        }

        @Test
        @DisplayName("vitória quando inimigo chega a 0 HP — sincroniza com game-api")
        void inimigoMorre_retornaVitoria() {
            batalhaEmAndamento.getInimigo().setHpAtual(1); // quase morto

            var request = new BatalhaRequest.ExecutarTurno(
                    BATALHA_ID, List.of(AcaoTurnoType.ATAQUE_FORTE), null
            );
            when(repositorio.findByIdAndUsuarioId(BATALHA_ID, USUARIO_ID))
                    .thenReturn(Optional.of(batalhaEmAndamento));
            when(gameApiFeign.sincronizarVitoria(any(), any()))
                    .thenReturn(ResponseEntity.ok().build());

            BatalhaResponse.Turno response = service.executarTurno(request, USUARIO_ID);

            assertThat(response.resultado()).isEqualTo(ResultadoBatalhaType.VITORIA);
            assertThat(response.batalhaEncerrada()).isTrue();
            assertThat(response.almasGanhas()).isPositive();
            verify(gameApiFeign).sincronizarVitoria(any(), any());
            verify(repositorio).deleteById(BATALHA_ID);
        }

        @Test
        @DisplayName("derrota quando personagem chega a 0 HP — notifica morte na game-api")
        void personagemMorre_retornaDerrota() {
            batalhaEmAndamento.getPersonagem().setHpAtual(1); // quase morto
            batalhaEmAndamento.getInimigo().setAtaque(500);   // inimigo mata com certeza

            var request = new BatalhaRequest.ExecutarTurno(
                    BATALHA_ID, List.of(AcaoTurnoType.ATACAR), null
            );
            when(repositorio.findByIdAndUsuarioId(BATALHA_ID, USUARIO_ID))
                    .thenReturn(Optional.of(batalhaEmAndamento));
            when(gameApiFeign.notificarMorte(any(), any()))
                    .thenReturn(ResponseEntity.ok(
                            new GameApiFeign.SoulDropResponse(99L, "Entrada da Catedral")
                    ));

            BatalhaResponse.Turno response = service.executarTurno(request, USUARIO_ID);

            assertThat(response.resultado()).isEqualTo(ResultadoBatalhaType.DERROTA);
            assertThat(response.batalhaEncerrada()).isTrue();
            assertThat(response.soulDropId()).isEqualTo(99L);
            verify(gameApiFeign).notificarMorte(any(), any());
            verify(repositorio).deleteById(BATALHA_ID);
        }

        @Test
        @DisplayName("USAR_ITEM passa o turno — inimigo ataca com bônus")
        void usarItem_passaTurno() {
            var request = new BatalhaRequest.ExecutarTurno(
                    BATALHA_ID, List.of(AcaoTurnoType.USAR_ITEM), 1L
            );
            when(repositorio.findByIdAndUsuarioId(BATALHA_ID, USUARIO_ID))
                    .thenReturn(Optional.of(batalhaEmAndamento));
            when(repositorio.save(any())).thenReturn(batalhaEmAndamento);

            BatalhaResponse.Turno response = service.executarTurno(request, USUARIO_ID);

            assertThat(response.danoCausado()).isZero();
            assertThat(response.danoRecebido()).isPositive();
            assertThat(response.batalhaEncerrada()).isFalse();
        }

        @Test
        @DisplayName("lança ObjectNotFoundException quando batalha não existe")
        void batalhaInexistente_lancaExcecao() {
            var request = new BatalhaRequest.ExecutarTurno(
                    "id-invalido", List.of(AcaoTurnoType.ATACAR), null
            );
            when(repositorio.findByIdAndUsuarioId("id-invalido", USUARIO_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.executarTurno(request, USUARIO_ID))
                    .isInstanceOf(ObjectNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────
    // buscarEstado
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("buscarEstado")
    class BuscarEstado {

        @Test
        @DisplayName("retorna estado completo da batalha")
        void batalhaExistente_retornaEstado() {
            when(repositorio.findByIdAndUsuarioId(BATALHA_ID, USUARIO_ID))
                    .thenReturn(Optional.of(batalhaEmAndamento));

            BatalhaResponse.Estado estado = service.buscarEstado(BATALHA_ID, USUARIO_ID);

            assertThat(estado.batalhaId()).isEqualTo(BATALHA_ID);
            assertThat(estado.nomeInimigo()).isEqualTo("Cavaleiro Oco");
            assertThat(estado.resultado()).isEqualTo(ResultadoBatalhaType.EM_ANDAMENTO);
        }

        @Test
        @DisplayName("lança ObjectNotFoundException quando batalha não existe")
        void batalhaInexistente_lancaExcecao() {
            when(repositorio.findByIdAndUsuarioId("id-invalido", USUARIO_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarEstado("id-invalido", USUARIO_ID))
                    .isInstanceOf(ObjectNotFoundException.class);
        }
    }
}