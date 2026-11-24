package com.br.meuimovel.repository;

import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.UsuarioStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImovelRepository extends JpaRepository<Imovel, Integer> {

    boolean existsByEmpresa_IdAndCodigo(Integer empresaId, String codigo);
    boolean existsByEmpresa_IdAndCodigoAndIdNot(Integer empresaId, String codigo, Integer id);

    List<Imovel> findAllByEmpresa_Id(Integer empresaId);

    Optional<Imovel> findByIdAndEmpresa_Id(Integer id, Integer empresaId);

    @Query(value = """
            SELECT imovel.*
            FROM imovel imovel
            LEFT JOIN endereco_imovel endereco ON endereco.imovel_id = imovel.id
            LEFT JOIN apartamento apartamento ON apartamento.imovel_id = imovel.id
            LEFT JOIN predio predio ON predio.id = apartamento.predio_id
            LEFT JOIN casa casa ON casa.imovel_id = imovel.id
            LEFT JOIN terreno terreno ON terreno.imovel_id = imovel.id
            LEFT JOIN lote lote ON lote.imovel_id = imovel.id
            WHERE imovel.publicado = TRUE
              AND imovel.situacao = 'DISPONIVEL'
              AND EXISTS (
                  SELECT 1 FROM corretor corretor
                  JOIN usuario usuario ON usuario.id = corretor.usuario_id
                  WHERE corretor.usuario_id = imovel.corretor_id
                    AND usuario.status = 'ATIVO'
              )
              AND (endereco.cidade_id = :cidadeId
                   OR (endereco.id IS NULL AND predio.cidade_id = :cidadeId))
              AND (
                  :texto IS NULL
                  OR POSITION(LOWER(:texto) IN LOWER(imovel.titulo)) > 0
                  OR POSITION(LOWER(:texto) IN LOWER(COALESCE(imovel.descricao, ''))) > 0
                  OR POSITION(LOWER(:texto) IN LOWER(COALESCE(endereco.bairro, predio.bairro, ''))) > 0
              )
              AND (
                  :finalidade IS NULL
                  OR imovel.finalidade = :finalidade
                  OR (:finalidade = 'VENDA' AND imovel.finalidade = 'VENDA_E_ALUGUEL')
                  OR (:finalidade = 'ALUGUEL' AND imovel.finalidade = 'VENDA_E_ALUGUEL')
              )
              AND (:natureza IS NULL OR imovel.natureza = :natureza)
              AND (:bairro IS NULL OR LOWER(COALESCE(endereco.bairro, predio.bairro, '')) = LOWER(:bairro))
              AND (
                  :precoMinimo IS NULL
                  OR CASE
                      WHEN :finalidade = 'ALUGUEL' THEN imovel.valor_aluguel
                      WHEN :finalidade IS NULL THEN COALESCE(imovel.valor_venda, imovel.valor_aluguel)
                      ELSE imovel.valor_venda
                  END >= :precoMinimo
              )
              AND (
                  :precoMaximo IS NULL
                  OR CASE
                      WHEN :finalidade = 'ALUGUEL' THEN imovel.valor_aluguel
                      WHEN :finalidade IS NULL THEN COALESCE(imovel.valor_venda, imovel.valor_aluguel)
                      ELSE imovel.valor_venda
                  END <= :precoMaximo
              )
              AND (:quartos IS NULL OR COALESCE(casa.quartos, apartamento.quartos) >= :quartos)
              AND (:suites IS NULL OR COALESCE(casa.suites, apartamento.suites) >= :suites)
              AND (:vagas IS NULL OR COALESCE(casa.vagas, apartamento.vagas) >= :vagas)
              AND (
                  :areaMinima IS NULL
                  OR COALESCE(casa.area_construida, terreno.area_total, apartamento.area_privativa, lote.area)
                    >= :areaMinima
              )
              AND (
                  :areaMaxima IS NULL
                  OR COALESCE(casa.area_construida, terreno.area_total, apartamento.area_privativa, lote.area)
                    <= :areaMaxima
              )
              AND (
                  :caracteristicasCsv = ''
                  OR (
                      SELECT COUNT(DISTINCT associacao.caracteristica_id)
                      FROM imovel_caracteristica associacao
                      WHERE associacao.imovel_id = imovel.id
                        AND POSITION(',' || CAST(associacao.caracteristica_id AS TEXT) || ','
                                     IN ',' || :caracteristicasCsv || ',') > 0
                  ) = :quantidadeCaracteristicas
              )
            ORDER BY
              CASE WHEN :ordenacao = 'MENOR_PRECO' THEN
                  CASE
                      WHEN :finalidade = 'ALUGUEL' THEN imovel.valor_aluguel
                      WHEN :finalidade IS NULL THEN COALESCE(imovel.valor_venda, imovel.valor_aluguel)
                      ELSE imovel.valor_venda
                  END
              END ASC NULLS LAST,
              CASE WHEN :ordenacao = 'MAIOR_PRECO' THEN
                  CASE
                      WHEN :finalidade = 'ALUGUEL' THEN imovel.valor_aluguel
                      WHEN :finalidade IS NULL THEN COALESCE(imovel.valor_venda, imovel.valor_aluguel)
                      ELSE imovel.valor_venda
                  END
              END DESC NULLS LAST,
              CASE WHEN :ordenacao = 'MENOR_AREA' THEN
                  COALESCE(casa.area_construida, terreno.area_total, apartamento.area_privativa, lote.area)
              END ASC NULLS LAST,
              CASE WHEN :ordenacao = 'MAIOR_AREA' THEN
                  COALESCE(casa.area_construida, terreno.area_total, apartamento.area_privativa, lote.area)
              END DESC NULLS LAST,
              imovel.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM imovel imovel
            LEFT JOIN endereco_imovel endereco ON endereco.imovel_id = imovel.id
            LEFT JOIN apartamento apartamento ON apartamento.imovel_id = imovel.id
            LEFT JOIN predio predio ON predio.id = apartamento.predio_id
            LEFT JOIN casa casa ON casa.imovel_id = imovel.id
            LEFT JOIN terreno terreno ON terreno.imovel_id = imovel.id
            LEFT JOIN lote lote ON lote.imovel_id = imovel.id
            WHERE imovel.publicado = TRUE
              AND imovel.situacao = 'DISPONIVEL'
              AND EXISTS (
                  SELECT 1 FROM corretor corretor
                  JOIN usuario usuario ON usuario.id = corretor.usuario_id
                  WHERE corretor.usuario_id = imovel.corretor_id
                    AND usuario.status = 'ATIVO'
              )
              AND (endereco.cidade_id = :cidadeId
                   OR (endereco.id IS NULL AND predio.cidade_id = :cidadeId))
              AND (
                  :texto IS NULL
                  OR POSITION(LOWER(:texto) IN LOWER(imovel.titulo)) > 0
                  OR POSITION(LOWER(:texto) IN LOWER(COALESCE(imovel.descricao, ''))) > 0
                  OR POSITION(LOWER(:texto) IN LOWER(COALESCE(endereco.bairro, predio.bairro, ''))) > 0
              )
              AND (
                  :finalidade IS NULL
                  OR imovel.finalidade = :finalidade
                  OR (:finalidade = 'VENDA' AND imovel.finalidade = 'VENDA_E_ALUGUEL')
                  OR (:finalidade = 'ALUGUEL' AND imovel.finalidade = 'VENDA_E_ALUGUEL')
              )
              AND (:natureza IS NULL OR imovel.natureza = :natureza)
              AND (:bairro IS NULL OR LOWER(COALESCE(endereco.bairro, predio.bairro, '')) = LOWER(:bairro))
              AND (
                  :precoMinimo IS NULL
                  OR CASE
                      WHEN :finalidade = 'ALUGUEL' THEN imovel.valor_aluguel
                      WHEN :finalidade IS NULL THEN COALESCE(imovel.valor_venda, imovel.valor_aluguel)
                      ELSE imovel.valor_venda
                  END >= :precoMinimo
              )
              AND (
                  :precoMaximo IS NULL
                  OR CASE
                      WHEN :finalidade = 'ALUGUEL' THEN imovel.valor_aluguel
                      WHEN :finalidade IS NULL THEN COALESCE(imovel.valor_venda, imovel.valor_aluguel)
                      ELSE imovel.valor_venda
                  END <= :precoMaximo
              )
              AND (:quartos IS NULL OR COALESCE(casa.quartos, apartamento.quartos) >= :quartos)
              AND (:suites IS NULL OR COALESCE(casa.suites, apartamento.suites) >= :suites)
              AND (:vagas IS NULL OR COALESCE(casa.vagas, apartamento.vagas) >= :vagas)
              AND (
                  :areaMinima IS NULL
                  OR COALESCE(casa.area_construida, terreno.area_total, apartamento.area_privativa, lote.area)
                    >= :areaMinima
              )
              AND (
                  :areaMaxima IS NULL
                  OR COALESCE(casa.area_construida, terreno.area_total, apartamento.area_privativa, lote.area)
                    <= :areaMaxima
              )
              AND (
                  :caracteristicasCsv = ''
                  OR (
                      SELECT COUNT(DISTINCT associacao.caracteristica_id)
                      FROM imovel_caracteristica associacao
                      WHERE associacao.imovel_id = imovel.id
                        AND POSITION(',' || CAST(associacao.caracteristica_id AS TEXT) || ','
                                     IN ',' || :caracteristicasCsv || ',') > 0
                  ) = :quantidadeCaracteristicas
              )
              AND :ordenacao IN ('MAIS_RECENTES', 'MENOR_PRECO', 'MAIOR_PRECO', 'MENOR_AREA', 'MAIOR_AREA')
            """,
            nativeQuery = true)
    Page<Imovel> buscarVitrine(
            @Param("cidadeId") Integer cidadeId,
            @Param("texto") String texto,
            @Param("finalidade") String finalidade,
            @Param("natureza") String natureza,
            @Param("bairro") String bairro,
            @Param("precoMinimo") BigDecimal precoMinimo,
            @Param("precoMaximo") BigDecimal precoMaximo,
            @Param("quartos") Integer quartos,
            @Param("suites") Integer suites,
            @Param("vagas") Integer vagas,
            @Param("areaMinima") BigDecimal areaMinima,
            @Param("areaMaxima") BigDecimal areaMaxima,
            @Param("caracteristicasCsv") String caracteristicasCsv,
            @Param("quantidadeCaracteristicas") Integer quantidadeCaracteristicas,
            @Param("ordenacao") String ordenacao,
            Pageable pageable);

    Optional<Imovel> findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
            Integer id, SituacaoImovel situacao, UsuarioStatus status);

}
