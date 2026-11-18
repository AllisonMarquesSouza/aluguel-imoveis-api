package com.br.meuimovel.repository;

import com.br.meuimovel.model.HistoricoSituacaoImovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoSituacaoImovelRepository extends JpaRepository<HistoricoSituacaoImovel, Integer> {
    List<HistoricoSituacaoImovel> findAllByImovel_IdOrderByAlteradoEmAscIdAsc(Integer imovelId);
}
