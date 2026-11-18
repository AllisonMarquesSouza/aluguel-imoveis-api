package com.br.meuimovel.repository;

import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.UsuarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImovelRepository extends JpaRepository<Imovel, Integer> {

    boolean existsByEmpresa_IdAndCodigo(Integer empresaId, String codigo);

    List<Imovel> findAllByEmpresa_Id(Integer empresaId);

    Optional<Imovel> findByIdAndEmpresa_Id(Integer id, Integer empresaId);

    List<Imovel> findAllByPublicadoTrueAndSituacaoAndCorretor_Usuario_StatusOrderByIdDesc(
            SituacaoImovel situacao, UsuarioStatus status);

    Optional<Imovel> findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
            Integer id, SituacaoImovel situacao, UsuarioStatus status);

}
