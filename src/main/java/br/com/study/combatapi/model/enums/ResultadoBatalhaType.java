package br.com.study.combatapi.model.enums;

/**
 * Estado atual ou resultado final de uma batalha.
 */
public enum ResultadoBatalhaType {

    /**
     * Batalha em andamento
     */
    EM_ANDAMENTO,

    /**
     * Jogador venceu — inimigo chegou a 0 HP
     */
    VITORIA,

    /**
     * Jogador morreu — HP chegou a 0
     */
    DERROTA,

    /**
     * Jogador fugiu (se futura funcionalidade for adicionada)
     */
    FUGA
}