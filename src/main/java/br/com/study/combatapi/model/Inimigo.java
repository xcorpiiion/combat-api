package br.com.study.combatapi.model;

import br.com.study.combatapi.model.enums.CategoriaInimigoType;
import lombok.Getter;
import lombok.Setter;

/**
 * Dados do inimigo dentro da batalha.
 * Documento embutido — não tem collection própria.
 * Gerado pela narrative-api e enviado ao iniciar a batalha.
 */
@Getter
@Setter
public class Inimigo {

    private String nome;
    private String descricao;
    private CategoriaInimigoType categoria;

    private int hpMaximo;
    private int hpAtual;
    private int ataque;
    private int defesa;

    /** Almas que o jogador ganha ao derrotá-lo */
    private long recompensaAlmas;

    public boolean estaMorto() {
        return hpAtual <= 0;
    }

    public void receberDano(int dano) {
        this.hpAtual = Math.max(0, this.hpAtual - dano);
    }
}