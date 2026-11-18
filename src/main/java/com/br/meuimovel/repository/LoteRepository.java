package com.br.meuimovel.repository;

import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Integer> {
    boolean existsByQuadra_IdAndNumeroIgnoreCase(Integer quadraId, String numero);

    long countByQuadra_IdAndSituacao(Integer quadraId, SituacaoImovel situacao);

    Optional<Lote> findByIdAndImovel_Empresa_Id(Integer id, Integer empresaId);
}
