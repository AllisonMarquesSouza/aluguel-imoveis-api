package com.br.meuimovel.repository;

import com.br.meuimovel.model.FotoImovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FotoImovelRepository extends JpaRepository<FotoImovel, Integer> {
    List<FotoImovel> findAllByImovel_IdOrderByOrdemAsc(Integer imovelId);//ja traz em ordem para o anuncio
    Optional<FotoImovel> findByImovel_IdAndUrl(Integer imovelId, String url);
}
