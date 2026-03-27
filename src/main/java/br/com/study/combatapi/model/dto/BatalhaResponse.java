package br.com.study.combatapi.model.dto;

import br.com.study.combatapi.model.enums.CategoriaInimigoType;
import br.com.study.combatapi.model.enums.ResultadoBatalhaType;

public class BatalhaResponse {

    /**
     * Retornado ao iniciar a batalha.
     * O front usa isso pra montar a tela de combate.
     */
    public record Inicio(
            String batalhaId,
            String sessaoId,

            // Inimigo
            String nomeInimigo,
            String descricaoInimigo,
            CategoriaInimigoType categoriaInimigo,
            int hpInimigo,
            int ataqueInimigo,
            int defesaInimigo,
            long recompensaAlmas,

            // Estado inicial do personagem
            int hpPersonagem,
            int mpPersonagem,
            int turnoAtual
    ) {
    }

    /**
     * Retornado após cada turno executado.
     * Contém a narração do GM + estado atualizado da batalha.
     */
    public record Turno(
            // Resultado do turno
            int danoCausado,
            int danoRecebido,
            boolean critico,

            // Estado atualizado
            int hpPersonagemAtual,
            int mpPersonagemAtual,
            int hpInimigoAtual,
            int turnoAtual,

            // Fim de batalha
            ResultadoBatalhaType resultado,
            boolean batalhaEncerrada,

            // Preenchido só em derrota
            Long soulDropId,
            String localizacaoSoulDrop,

            // Preenchido só em vitória
            Long almasGanhas
    ) {
    }

    /**
     * Estado completo da batalha — usado pra reconectar
     * caso o jogador feche e reabra o app durante uma batalha.
     */
    public record Estado(
            String batalhaId,
            int turnoAtual,
            ResultadoBatalhaType resultado,

            // Personagem
            int hpPersonagemAtual,
            int hpPersonagemMaximo,
            int mpPersonagemAtual,
            int mpPersonagemMaximo,

            // Inimigo
            String nomeInimigo,
            CategoriaInimigoType categoriaInimigo,
            int hpInimigoAtual,
            int hpInimigoMaximo
    ) {
    }
}