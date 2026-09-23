package com.br.meuimovel.repository;

import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.model.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CidadeRepository extends JpaRepository<Cidade, Integer> {
    boolean existsByNomeIgnoreCaseAndUf(String nome, Uf uf);
//    boolean existsAllById
}
