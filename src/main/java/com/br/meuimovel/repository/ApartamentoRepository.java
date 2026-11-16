package com.br.meuimovel.repository;

import com.br.meuimovel.model.Apartamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApartamentoRepository extends JpaRepository<Apartamento, Integer> {
    boolean existsByPredio_IdAndAndarAndNumeroIgnoreCase(Integer predioId, Integer andar, String numero);
}
