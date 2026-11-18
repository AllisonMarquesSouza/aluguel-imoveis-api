package com.br.meuimovel.service;

import com.br.meuimovel.dtos.caracteristica.CaracteristicaResponseDto;
import com.br.meuimovel.dtos.imovel.*;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.TipoImagem;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.mapper.LoteMapper;
import com.br.meuimovel.mapper.PredioMapper;
import com.br.meuimovel.model.EnderecoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.repository.FotoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.storage.ImagemUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImovelVitrineService {
    private final ImovelRepository imovelRepository;
    private final FotoImovelRepository fotoImovelRepository;

    @Transactional(readOnly = true)
    public List<ImovelVitrineResponseDto> listarDisponiveis() {
        return imovelRepository.findAllByPublicadoTrueAndSituacaoAndCorretor_Usuario_StatusOrderByIdDesc(
                        SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ImovelVitrineResponseDto buscarDisponivel(Integer id) {
        Imovel imovel = imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                        id, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)
                .orElseThrow(() -> new ImovelNotFoundException("Imóvel não encontrado na vitrine."));
        return toResponse(imovel);
    }

    private ImovelVitrineResponseDto toResponse(Imovel imovel) {
        EnderecoImovel endereco = imovel.getEndereco();
        EnderecoImovelResponseDto enderecoResponse = endereco == null
                ? PredioMapper.toImovelEndereco(imovel.getApartamento().getPredio())
                : new EnderecoImovelResponseDto(endereco.getId(), endereco.getCep(), endereco.getLogradouro(),
                endereco.getNumero(), endereco.getComplemento(), endereco.getBairro(), endereco.getCidade().getId(),
                endereco.getCidade().getNome(), endereco.getCidade().getUf().name(),
                endereco.getLatitude(), endereco.getLongitude());

        Set<CaracteristicaResponseDto> caracteristicas = imovel.getCaracteristicas().stream()
                .map(caracteristica -> new CaracteristicaResponseDto(caracteristica.getId(), caracteristica.getNome()))
                .collect(Collectors.toSet());
        List<FotoImovelResponseDto> fotos = fotoImovelRepository
                .findAllByImovel_IdOrderByOrdemAsc(imovel.getId()).stream()
                .map(foto -> new FotoImovelResponseDto(
                        foto.getId(), ImagemUrl.extrairImagemId(foto.getUrl(), TipoImagem.IMOVEL, imovel.getId()),
                        foto.getUrl(), foto.getOrdem(), foto.isCapa()))
                .toList();

        CasaResponseDto casa = imovel.getCasa() == null ? null : new CasaResponseDto(
                imovel.getCasa().getAreaTerreno(), imovel.getCasa().getAreaConstruida(),
                imovel.getCasa().getAreaTerreno().subtract(imovel.getCasa().getAreaConstruida()),
                imovel.getCasa().getQuartos(), imovel.getCasa().getSuites(), imovel.getCasa().getBanheiros(),
                imovel.getCasa().getSalas(), imovel.getCasa().getVagas(), imovel.getCasa().getPavimentos(),
                imovel.getCasa().getAnoConstrucao(), imovel.getCasa().getPiscina(), imovel.getCasa().getQuintal(),
                imovel.getCasa().getChurrasqueira(), imovel.getCasa().getMobiliada());
        TerrenoResponseDto terreno = imovel.getTerreno() == null ? null : new TerrenoResponseDto(
                imovel.getTerreno().getAreaTotal(), imovel.getTerreno().getFrente(), imovel.getTerreno().getFundo(),
                imovel.getTerreno().getLateralEsquerda(), imovel.getTerreno().getLateralDireita(),
                imovel.getTerreno().getTopografia(), imovel.getTerreno().getMuro(), imovel.getTerreno().getEsquina(),
                imovel.getTerreno().getUsoPermitido());
        ApartamentoResponseDto apartamento = imovel.getApartamento() == null ? null : new ApartamentoResponseDto(
                imovel.getApartamento().getAndar(), imovel.getApartamento().getNumero(),
                imovel.getApartamento().getAreaPrivativa(), imovel.getApartamento().getAreaTotal(),
                imovel.getApartamento().getQuartos(), imovel.getApartamento().getSuites(),
                imovel.getApartamento().getBanheiros(), imovel.getApartamento().getVagas(),
                imovel.getApartamento().getVaranda(), imovel.getApartamento().getPosicaoSolar(),
                PredioMapper.toResponse(imovel.getApartamento().getPredio()));
        LoteResponseDto lote = imovel.getLote() == null ? null : LoteMapper.toResponse(imovel.getLote());

        return new ImovelVitrineResponseDto(
                imovel.getId(), imovel.getCodigo(), imovel.getTitulo(), imovel.getFinalidade(), imovel.getNatureza(),
                casa, terreno, apartamento, lote, imovel.getValorVenda(), imovel.getValorAluguel(),
                imovel.getValorCondominio(), imovel.getValorIptu(), imovel.getDescricao(), enderecoResponse,
                caracteristicas, fotos);
    }
}
