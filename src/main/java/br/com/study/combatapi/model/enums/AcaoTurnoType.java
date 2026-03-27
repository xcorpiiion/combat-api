package br.com.study.combatapi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ações disponíveis para montar o combo de um turno.
 * <p>
 * Custo de AP:
 * ATACAR          = 1 AP
 * ATAQUE_FORTE    = 2 AP
 * ESQUIVAR        = 1 AP
 * DEFENDER        = 1 AP
 * ESPECIAL        = 0 AP (custa 3 MP)
 * USAR_ITEM       = 0 AP (passa o turno inteiro)
 * <p>
 * Regras do combo:
 * - A soma dos custos de AP não pode ultrapassar o AP máximo do personagem
 * - USAR_ITEM descarta todo o resto do combo — o turno é cedido ao inimigo
 * - ESPECIAL pode aparecer uma vez por turno
 */
@Getter
@RequiredArgsConstructor
public enum AcaoTurnoType {

    ATACAR(1, 0, "Ataque básico baseado em ATQ"),
    ATAQUE_FORTE(2, 0, "Ataque poderoso — dano x1.8, porém custa 2 AP"),
    ESQUIVAR(1, 0, "Reduz em 50% o dano do próximo ataque inimigo"),
    DEFENDER(1, 0, "Reduz em 40% todo o dano recebido neste turno"),
    ESPECIAL(0, 3, "Habilidade única da classe — custa 3 MP"),
    USAR_ITEM(0, 0, "Usa um item consumível — passa o turno ao inimigo");

    private final int custAp;
    private final int custoMp;
    private final String descricao;
}