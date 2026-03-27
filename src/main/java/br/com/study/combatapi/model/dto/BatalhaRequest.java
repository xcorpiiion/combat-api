package br.com.study.combatapi.model.dto;

import br.com.study.combatapi.model.enums.AcaoTurnoType;
import br.com.study.combatapi.model.enums.CategoriaInimigoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BatalhaRequest {

    public record IniciarBatalha(
            @NotNull Long personagemId,
            @NotBlank String sessaoId,
            @NotBlank String nomePersonagem,
            @NotNull int hpAtual,
            @NotNull int hpMaximo,
            @NotNull int mpAtual,
            @NotNull int mpMaximo,
            @NotNull int apMaximo,
            @NotNull int ataque,
            @NotNull int defesa,
            @NotNull int velocidade,
            @NotNull int sorte,

            // Simulacro do Vazio
            long bitsConsciencia,
            boolean hollow,
            boolean temMemoriasDisponiveis,

            // Inimigo gerado pelo GM
            @NotBlank String nomeInimigo,
            String descricaoInimigo,
            @NotNull CategoriaInimigoType categoriaInimigo,
            @NotNull int hpInimigo,
            @NotNull int ataqueInimigo,
            @NotNull int defesaInimigo,
            long recompensaAlmas
    ) {
    }

    public record ExecutarTurno(
            @NotNull String batalhaId,
            @NotEmpty List<AcaoTurnoType> combo,
            Long itemId
    ) {
    }
}