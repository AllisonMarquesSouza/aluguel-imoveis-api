package com.br.meuimovel.repository;

import com.br.meuimovel.model.Quadra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuadraRepository extends JpaRepository<Quadra, Integer> {
    List<Quadra> findAllByLoteamento_IdOrderByIdentificacaoAsc(Integer loteamentoId);

    Optional<Quadra> findByIdAndLoteamento_Empresa_Id(Integer id, Integer empresaId);

    boolean existsByLoteamento_IdAndIdentificacaoIgnoreCase(Integer loteamentoId, String identificacao);
}
