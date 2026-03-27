package br.com.study.combatapi.model.dto;

import br.com.study.combatapi.model.enums.CategoriaInimigoType;
import br.com.study.combatapi.model.enums.ResultadoBatalhaType;

public class BatalhaResponse {

    public record Inicio(
            String batalhaId,
            String sessaoId,
            String nomeInimigo,
            String descricaoInimigo,
            CategoriaInimigoType categoriaInimigo,
            int hpInimigo,
            int ataqueInimigo,
            int defesaInimigo,
            long recompensaAlmas,
            int hpPersonagem,
            int mpPersonagem,
            int turnoAtual
    ) {
    }

    public record Turno(
            int danoCausado,
            int danoRecebido,
            boolean critico,

            /**
             * Descrição da memória queimada no crítico.
             * Enviada pra narrative-api narrar o que foi perdido.
             * Null se não houve crítico ou sem memórias disponíveis.
             */
            String memoriaQueimada,

            int hpPersonagemAtual,
            int mpPersonagemAtual,
            int hpInimigoAtual,
            int turnoAtual,
            ResultadoBatalhaType resultado,
            boolean batalhaEncerrada,

            // Derrota normal
            Long soulDropId,
            String localizacaoSoulDrop,

            // Vitória
            Long bitsConscienciaGanhos
    ) {
    }

    public record Estado(
            String batalhaId,
            int turnoAtual,
            ResultadoBatalhaType resultado,
            int hpPersonagemAtual,
            int hpPersonagemMaximo,
            int mpPersonagemAtual,
            int mpPersonagemMaximo,
            String nomeInimigo,
            CategoriaInimigoType categoriaInimigo,
            int hpInimigoAtual,
            int hpInimigoMaximo
    ) {
    }
}