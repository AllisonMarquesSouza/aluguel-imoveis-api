package com.br.meuimovel.repository;

import com.br.meuimovel.model.Caracteristica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaracteristicaRepository extends JpaRepository<Caracteristica, Integer> {
    Optional<Caracteristica> findByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCase(String nome);
}
