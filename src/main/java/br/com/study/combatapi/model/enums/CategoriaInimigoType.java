package br.com.study.combatapi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Categoria do inimigo — define o multiplicador de atributos
 * em cima dos valores base gerados pela narrative-api.
 * <p>
 * Normal → inimigo padrão
 * Elite  → mais forte, mais almas
 * Chefe  → batalha especial, muito mais difícil
 */
@Getter
@RequiredArgsConstructor
public enum CategoriaInimigoType {

    /**
     * Inimigo comum — encontrado com frequência.
     * Sem multiplicador nos atributos.
     */
    NORMAL(1.0, 1.0, "Inimigo comum"),

    /**
     * Versão mais poderosa do inimigo comum.
     * 50% mais HP e ATQ, recompensa 2x mais almas.
     */
    ELITE(1.5, 2.0, "Inimigo poderoso — recompensa maior"),

    /**
     * Chefe de área — batalha especial.
     * Atributos dobrados, recompensa 5x mais almas.
     * Normalmente único por região.
     */
    CHEFE(2.0, 5.0, "Chefe — batalha épica");

    /**
     * Multiplicador aplicado sobre HP e ATQ base
     */
    private final double multiplicadorAtributos;

    /**
     * Multiplicador aplicado sobre a recompensa de almas
     */
    private final double multiplicadorAlmas;

    private final String descricao;
}