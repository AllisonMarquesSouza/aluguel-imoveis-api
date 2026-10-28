package com.br.meuimovel.repository;

import com.br.meuimovel.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {
    boolean existsByDocumento(String documento);
    boolean existsByCreci(String creci);
    Optional<Empresa> findByIdAndLogoMarcaUrl(Integer id, String logoMarcaUrl);

    @Query("select count(c) > 0 from Empresa e join e.cidadesAtuacao c " +
            "where e.id = :empresaId and c.id = :cidadeId")
    boolean atuaNaCidade(@Param("empresaId") Integer empresaId,
                         @Param("cidadeId") Integer cidadeId);
}
