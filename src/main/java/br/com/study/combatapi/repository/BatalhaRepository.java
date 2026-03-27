package br.com.study.combatapi.repository;

import br.com.study.combatapi.model.Batalha;
import br.com.study.combatapi.model.enums.ResultadoBatalhaType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatalhaRepository extends MongoRepository<Batalha, String> {

    /**
     * Busca a batalha ativa de um personagem.
     * Um personagem só pode ter uma batalha em andamento por vez.
     */
    Optional<Batalha> findByPersonagem_PersonagemIdAndResultado(
            Long personagemId,
            ResultadoBatalhaType resultado
    );

    /**
     * Verifica se o personagem já está em batalha.
     */
    boolean existsByPersonagem_PersonagemIdAndResultado(
            Long personagemId,
            ResultadoBatalhaType resultado
    );

    /**
     * Busca batalha ativa garantindo que pertence ao usuário correto.
     * Segurança: impede um usuário de interagir com batalha de outro.
     */
    Optional<Batalha> findByIdAndUsuarioId(String id, Long usuarioId);
}