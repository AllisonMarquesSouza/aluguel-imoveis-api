package com.br.meuimovel.repository;

import com.br.meuimovel.model.ContatoImovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContatoImovelRepository extends JpaRepository<ContatoImovel, Integer> {
    @Query("""
            select contato from ContatoImovel contato
            where contato.imovel.empresa.id = :empresaId
              and (:corretorId is null or contato.corretor.id = :corretorId)
              and (:imovelId is null or contato.imovel.id = :imovelId)
              and (:inicio is null or contato.criadoEm >= :inicio)
              and (:fimExclusivo is null or contato.criadoEm < :fimExclusivo)
            order by contato.criadoEm desc, contato.id desc
            """)
    List<ContatoImovel> listarCaixa(
            @Param("empresaId") Integer empresaId,
            @Param("corretorId") Integer corretorId,
            @Param("imovelId") Integer imovelId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo);

    Optional<ContatoImovel> findByIdAndCorretor_IdAndImovel_Empresa_Id(
            Integer id, Integer corretorId, Integer empresaId);
}
