package com.br.meuimovel.repository;

import com.br.meuimovel.model.Loteamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoteamentoRepository extends JpaRepository<Loteamento, Integer> {
    List<Loteamento> findAllByEmpresa_IdOrderByNomeAsc(Integer empresaId);

    Optional<Loteamento> findByIdAndEmpresa_Id(Integer id, Integer empresaId);
}
