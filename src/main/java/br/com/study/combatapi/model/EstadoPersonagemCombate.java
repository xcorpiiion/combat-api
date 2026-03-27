package br.com.study.combatapi.model;

import br.com.study.combatapi.model.enums.AcaoTurnoType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot dos atributos do personagem durante a batalha.
 * Documento embutido — atualizado a cada turno.
 * <p>
 * Guardamos uma cópia aqui para não precisar chamar a game-api
 * a cada turno. Ao fim da batalha, sincronizamos o resultado
 * de volta pra game-api via Feign.
 */
@Getter
@Setter
public class EstadoPersonagemCombate {

    private Long personagemId;
    private String nome;

    // Vitais
    private int hpAtual;
    private int hpMaximo;
    private int mpAtual;
    private int mpMaximo;
    private int apMaximo;

    // Combate
    private int ataque;
    private int defesa;
    private int velocidade;
    private int sorte;

    // Efeitos ativos no turno atual
    /**
     * Se true, próximo ataque inimigo causa 50% menos dano
     */
    private boolean esquivaAtiva = false;

    /**
     * Se true, todo dano recebido neste turno é reduzido em 40%
     */
    private boolean defesaAtiva = false;

    /**
     * Ações do combo do turno atual.
     * Limpo ao início de cada turno.
     */
    private List<AcaoTurnoType> comboAtual = new ArrayList<>();

    public boolean estaMorto() {
        return hpAtual <= 0;
    }

    public void receberDano(int dano) {
        int danoFinal = dano;

        if (esquivaAtiva) danoFinal = danoFinal / 2;
        if (defesaAtiva) danoFinal = (int) (danoFinal * 0.6);

        danoFinal = Math.max(1, danoFinal - defesa);
        this.hpAtual = Math.max(0, this.hpAtual - danoFinal);
    }

    public void gastarMp(int quantidade) {
        this.mpAtual = Math.max(0, this.mpAtual - quantidade);
    }

    public void limparEfeitosTurno() {
        this.esquivaAtiva = false;
        this.defesaAtiva = false;
        this.comboAtual = new ArrayList<>();
    }
}