package com.br.meuimovel.repository;

import com.br.meuimovel.enums.TipoDocumentoImovel;
import com.br.meuimovel.model.DocumentoImovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoImovelRepository extends JpaRepository<DocumentoImovel, Integer> {
    boolean existsByImovel_IdAndTipo(Integer imovelId, TipoDocumentoImovel tipo);

    List<DocumentoImovel> findAllByImovel_IdOrderByTipoAsc(Integer imovelId);

    Optional<DocumentoImovel> findByIdAndImovel_Id(Integer id, Integer imovelId);
}
