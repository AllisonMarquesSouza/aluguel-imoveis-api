package com.br.meuimovel.repository;

import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.model.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CidadeRepository extends JpaRepository<Cidade, Integer> {
    boolean existsByNomeIgnoreCaseAndUf(String nome, Uf uf);
    Optional<Cidade> findByNomeIgnoreCaseAndUf(String nome, Uf uf);

    @Query("""
            select distinct cidade from Empresa empresa
            join empresa.cidadesAtuacao cidade
            where empresa.ativa = true
            order by cidade.nome, cidade.uf
            """)
    List<Cidade> listarCidadesAtendidas();

    @Query("""
            select distinct cidade from Empresa empresa
            join empresa.cidadesAtuacao cidade
            where empresa.id = :empresaId
            order by cidade.nome, cidade.uf
            """)
    List<Cidade> listarPorEmpresaId(@Param("empresaId") Integer empresaId);
}
