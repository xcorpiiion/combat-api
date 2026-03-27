package br.com.study.combatapi.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "game-api")
public interface GameApiFeign {

    /**
     * Notifica morte normal — personagem tinha Bits suficientes.
     * game-api cria o SoulDrop com os Bits perdidos.
     */
    @PostMapping(value = "/personagens/{personagemId}/morte",
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<SoulDropResponse> notificarMorte(
            @PathVariable Long personagemId,
            @RequestBody MorteRequest request
    );

    /**
     * Notifica Hollow Digital — morreu sem Bits.
     * game-api marca hollow=true e queima todas as memórias restantes.
     */
    @PostMapping(value = "/personagens/{personagemId}/hollow",
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<Void> notificarHollow(@PathVariable Long personagemId);

    /**
     * Queima uma memória ao executar crítico.
     * Retorna a descrição da memória perdida pro Gemini narrar.
     */
    @PostMapping(value = "/personagens/{personagemId}/memorias/queimar",
            produces = APPLICATION_JSON_VALUE)
    ResponseEntity<MemoriaQueimadaResponse> queimarMemoria(
            @PathVariable Long personagemId
    );

    /**
     * Sincroniza HP, MP e Bits após vitória
     */
    @PutMapping(value = "/personagens/{personagemId}/sincronizar-batalha",
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<Void> sincronizarVitoria(
            @PathVariable Long personagemId,
            @RequestBody SincronizarVitoriaRequest request
    );

    // ─── DTOs internos ────────────────────────────────────────────────────

    record MorteRequest(long bitsConscienciaPerdidos, String localizacao) {
    }

    record SincronizarVitoriaRequest(
            int hpAtual, int mpAtual,
            long bitsConscienciaGanhos,
            long experienciaGanha
    ) {
    }

    record SoulDropResponse(Long soulDropId, String localizacao) {
    }

    record MemoriaQueimadaResponse(
            Long memoriaId,
            String tipo,
            String descricao
    ) {
    }
}