package br.com.study.combatapi.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Client Feign para a game-api.
 * Usado ao fim de cada batalha para sincronizar o resultado.
 */
@FeignClient(name = "game-api")
public interface GameApiFeign {

    /**
     * Notifica a game-api que o personagem morreu.
     * A game-api cria o SoulDrop e zera as almas.
     */
    @PostMapping(
            value = "/personagens/{personagemId}/morte",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<SoulDropResponse> notificarMorte(
            @PathVariable Long personagemId,
            @RequestBody MorteRequest request
    );

    /**
     * Sincroniza HP, MP e almas após vitória.
     */
    @PutMapping(
            value = "/personagens/{personagemId}/sincronizar-batalha",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> sincronizarVitoria(
            @PathVariable Long personagemId,
            @RequestBody SincronizarVitoriaRequest request
    );

    // ─── DTOs internos do Feign ───────────────────────────────────────────

    record MorteRequest(
            long almasPerdidas,
            String localizacao
    ) {
    }

    record SincronizarVitoriaRequest(
            int hpAtual,
            int mpAtual,
            long almasGanhas,
            long experienciaGanha
    ) {
    }

    record SoulDropResponse(
            Long soulDropId,
            String localizacao
    ) {
    }
}