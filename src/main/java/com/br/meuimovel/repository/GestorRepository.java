package com.br.meuimovel.repository;

import com.br.meuimovel.model.Gestor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GestorRepository extends JpaRepository<Gestor, Integer> {
}
