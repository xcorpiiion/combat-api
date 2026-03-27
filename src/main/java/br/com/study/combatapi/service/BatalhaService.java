package br.com.study.combatapi.service;


import br.com.study.combatapi.model.dto.BatalhaRequest;
import br.com.study.combatapi.model.dto.BatalhaResponse;

public interface BatalhaService {

    BatalhaResponse.Inicio iniciar(BatalhaRequest.IniciarBatalha request, Long usuarioId);

    BatalhaResponse.Turno executarTurno(BatalhaRequest.ExecutarTurno request, Long usuarioId);

    BatalhaResponse.Estado buscarEstado(String batalhaId, Long usuarioId);
}