package br.com.study.combatapi.controller;

import br.com.study.combatapi.model.dto.BatalhaRequest;
import br.com.study.combatapi.model.dto.BatalhaResponse;
import br.com.study.combatapi.service.BatalhaService;
import br.com.study.genericauthorization.annotation.CurrentUser;
import br.com.study.genericauthorization.model.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batalhas")
@Tag(name = "Batalhas", description = "Motor de combate por turnos")
public class BatalhaController {

    private final BatalhaService service;

    @Operation(summary = "Inicia uma batalha — chamado pela narrative-api após gerar o inimigo")
    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<BatalhaResponse.Inicio> iniciar(@Valid @RequestBody BatalhaRequest.IniciarBatalha request, @CurrentUser UserPrincipal usuario) {
        return ResponseEntity.status(CREATED).body(service.iniciar(request, usuario.getId()));
    }

    @Operation(summary = "Executa um turno com o combo de ações escolhido")
    @PostMapping(value = "/turno", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<BatalhaResponse.Turno> executarTurno(@Valid @RequestBody BatalhaRequest.ExecutarTurno request, @CurrentUser UserPrincipal usuario) {
        return ResponseEntity.ok(service.executarTurno(request, usuario.getId()));
    }

    @Operation(summary = "Busca o estado atual de uma batalha em andamento")
    @GetMapping(value = "/{batalhaId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<BatalhaResponse.Estado> buscarEstado(@PathVariable String batalhaId, @CurrentUser UserPrincipal usuario) {
        return ResponseEntity.ok(service.buscarEstado(batalhaId, usuario.getId()));
    }
}