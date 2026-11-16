package com.br.meuimovel.repository;

import com.br.meuimovel.model.Predio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredioRepository extends JpaRepository<Predio, Integer> {
    List<Predio> findAllByEmpresa_IdOrderByNomeAsc(Integer empresaId);

    Optional<Predio> findByIdAndEmpresa_Id(Integer id, Integer empresaId);
}
