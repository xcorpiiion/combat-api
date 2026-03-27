package br.com.study.combatapi.model;

import br.com.study.combatapi.model.enums.ResultadoBatalhaType;
import br.com.study.genericcrudmongo.model.AbstractDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


/**
 * Estado completo de uma batalha em andamento.
 * <p>
 * Ciclo de vida:
 * - Criado quando a narrative-api envia o inimigo gerado
 * - Atualizado a cada turno executado
 * - Deletado ao fim da batalha (vitória ou derrota)
 * <p>
 * Ao deletar, sincronizamos HP, MP e almas de volta
 * pra game-api via Feign.
 */
@Getter
@Setter
@Document(collection = "batalhas")
public class Batalha extends AbstractDocument {

    /**
     * ID do usuário dono do personagem
     */
    @Field("usuario_id")
    private Long usuarioId;

    /**
     * ID da sessão na narrative-api para manter contexto do GM
     */
    @Field("sessao_id")
    private String sessaoId;

    /**
     * Snapshot do personagem — atualizado a cada turno
     */
    private EstadoPersonagemCombate personagem;

    /**
     * Dados do inimigo gerado pela narrative-api
     */
    private Inimigo inimigo;

    /**
     * Turno atual — começa em 1
     */
    @Field("turno_atual")
    private int turnoAtual = 1;

    /**
     * Estado atual da batalha
     */
    private ResultadoBatalhaType resultado = ResultadoBatalhaType.EM_ANDAMENTO;

    public boolean estaEmAndamento() {
        return resultado == ResultadoBatalhaType.EM_ANDAMENTO;
    }

    public void avancarTurno() {
        this.turnoAtual++;
        this.personagem.limparEfeitosTurno();
    }

}