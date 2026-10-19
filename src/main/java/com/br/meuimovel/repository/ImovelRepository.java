package com.br.meuimovel.repository;

import com.br.meuimovel.model.Imovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImovelRepository extends JpaRepository<Imovel, Integer> {

    boolean existsByEmpresa_IdAndCodigo(Integer empresaId, String codigo);

    List<Imovel> findAllByEmpresa_Id(Integer empresaId);

    Optional<Imovel> findByIdAndEmpresa_Id(Integer id, Integer empresaId);

}
