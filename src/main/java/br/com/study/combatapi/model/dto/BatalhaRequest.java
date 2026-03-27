package br.com.study.combatapi.model.dto;

import br.com.study.combatapi.model.enums.AcaoTurnoType;
import br.com.study.combatapi.model.enums.CategoriaInimigoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BatalhaRequest {

    /**
     * Iniciada pela narrative-api quando o GM gera um inimigo.
     * Contém o snapshot do personagem e os dados do inimigo gerado.
     */
    public record IniciarBatalha(

            @NotNull(message = "ID do personagem é obrigatório")
            Long personagemId,

            @NotBlank(message = "ID da sessão é obrigatório")
            String sessaoId,

            // ─── Snapshot do personagem ───────────────────────
            @NotBlank(message = "Nome do personagem é obrigatório")
            String nomePersonagem,

            @NotNull int hpAtual,
            @NotNull int hpMaximo,
            @NotNull int mpAtual,
            @NotNull int mpMaximo,
            @NotNull int apMaximo,
            @NotNull int ataque,
            @NotNull int defesa,
            @NotNull int velocidade,
            @NotNull int sorte,

            // ─── Dados do inimigo gerado pelo GM ──────────────
            @NotBlank(message = "Nome do inimigo é obrigatório")
            String nomeInimigo,

            String descricaoInimigo,

            @NotNull(message = "Categoria do inimigo é obrigatória")
            CategoriaInimigoType categoriaInimigo,

            @NotNull int hpInimigo,
            @NotNull int ataqueInimigo,
            @NotNull int defesaInimigo,
            @NotNull long recompensaAlmas
    ) {
    }

    /**
     * Submissão do combo de ações do turno.
     * A soma dos custos de AP não pode ultrapassar o apMaximo do personagem.
     * Se o combo contiver USAR_ITEM, itemId é obrigatório.
     */
    public record ExecutarTurno(

            @NotNull(message = "ID da batalha é obrigatório")
            String batalhaId,

            @NotEmpty(message = "O combo não pode ser vazio")
            List<AcaoTurnoType> combo,

            /** Obrigatório quando o combo contém USAR_ITEM */
            Long itemId
    ) {
    }
}