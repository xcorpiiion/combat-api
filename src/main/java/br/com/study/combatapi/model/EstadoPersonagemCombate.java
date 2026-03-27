package br.com.study.combatapi.model;

import br.com.study.combatapi.model.enums.AcaoTurnoType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot do personagem durante a batalha.
 * <p>
 * Mudanças do Simulacro do Vazio:
 * - bitsConsciencia substitui almas
 * - memoriaQueimadaId: preenchido quando um crítico ocorre
 * - temMemoriasDisponiveis: controla se crítico é possível
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

    // Bits de Consciência
    private long bitsConsciencia;

    /**
     * Controla se o personagem ainda tem memórias pra queimar em críticos
     */
    private boolean temMemoriasDisponiveis;

    // Efeitos do turno
    private boolean esquivaAtiva = false;
    private boolean defesaAtiva = false;

    /**
     * Preenchido quando um crítico ocorre no turno.
     * Enviado pra narrative-api narrar qual memória foi perdida.
     */
    private String memoriaQueimadaDescricao;

    private List<AcaoTurnoType> comboAtual = new ArrayList<>();

    public boolean estaMorto() {
        return hpAtual <= 0;
    }

    public boolean temBits() {
        return bitsConsciencia > 0;
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
        this.memoriaQueimadaDescricao = null;
    }
}