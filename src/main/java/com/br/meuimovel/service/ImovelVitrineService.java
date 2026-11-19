package com.br.meuimovel.service;

import com.br.meuimovel.dtos.caracteristica.CaracteristicaResponseDto;
import com.br.meuimovel.dtos.corretor.CorretorVitrineResponseDto;
import com.br.meuimovel.dtos.imovel.*;
import com.br.meuimovel.dtos.predio.EnderecoPredioResponseDto;
import com.br.meuimovel.dtos.predio.PredioVitrineResponseDto;
import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.enums.OrdenacaoVitrine;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.TipoImagem;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CidadeNotFoundException;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.mapper.LoteMapper;
import com.br.meuimovel.model.Caracteristica;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.EnderecoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Predio;
import com.br.meuimovel.repository.CidadeRepository;
import com.br.meuimovel.repository.FotoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.storage.ImagemUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImovelVitrineService {
    private final ImovelRepository imovelRepository;
    private final FotoImovelRepository fotoImovelRepository;
    private final CidadeRepository cidadeRepository;

    @Transactional(readOnly = true)
    public List<CidadeResponseDto> listarCidades() {
        return cidadeRepository.listarCidadesAtendidas()
                .stream().map(this::toCidadeResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ImovelVitrineResponseDto> buscar(ImovelVitrineFiltro filtro) {
        validarFiltro(filtro);
        if (!cidadeRepository.existsById(filtro.cidadeId())) {
            throw new CidadeNotFoundException("Cidade não encontrada.");
        }

        ImovelVitrineFiltro filtroNormalizado = normalizar(filtro);
        List<Integer> caracteristicas = filtroNormalizado.caracteristicas() == null
                ? List.of() : filtroNormalizado.caracteristicas();
        String caracteristicasCsv = caracteristicas.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        Pageable pageable = PageRequest.of(filtroNormalizado.pagina(), filtroNormalizado.tamanho());
        return imovelRepository.buscarVitrine(
                        filtroNormalizado.cidadeId(), filtroNormalizado.texto(),
                        filtroNormalizado.finalidade() == null ? null : filtroNormalizado.finalidade().name(),
                        filtroNormalizado.natureza() == null ? null : filtroNormalizado.natureza().name(),
                        filtroNormalizado.bairro(), filtroNormalizado.precoMinimo(), filtroNormalizado.precoMaximo(),
                        filtroNormalizado.quartos(), filtroNormalizado.suites(), filtroNormalizado.vagas(),
                        filtroNormalizado.areaMinima(), filtroNormalizado.areaMaxima(),
                        caracteristicasCsv, caracteristicas.size(), filtroNormalizado.ordenacao().name(), pageable)
                .map(this::toResponse);
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
                ? toEnderecoImovelResponse(imovel.getApartamento().getPredio())
                : new EnderecoImovelResponseDto(null, endereco.getCep(), endereco.getLogradouro(),
                endereco.getNumero(), endereco.getComplemento(), endereco.getBairro(), endereco.getCidade().getId(),
                endereco.getCidade().getNome(), endereco.getCidade().getUf().name(),
                endereco.getLatitude(), endereco.getLongitude());

        Set<CaracteristicaResponseDto> caracteristicas = imovel.getCaracteristicas().stream()
                .sorted(java.util.Comparator.comparing(Caracteristica::getNome))
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
        ApartamentoVitrineResponseDto apartamento = imovel.getApartamento() == null ? null : new ApartamentoVitrineResponseDto(
                imovel.getApartamento().getAndar(), imovel.getApartamento().getNumero(),
                imovel.getApartamento().getAreaPrivativa(), imovel.getApartamento().getAreaTotal(),
                imovel.getApartamento().getQuartos(), imovel.getApartamento().getSuites(),
                imovel.getApartamento().getBanheiros(), imovel.getApartamento().getVagas(),
                imovel.getApartamento().getVaranda(), imovel.getApartamento().getPosicaoSolar(),
                toPredioResponse(imovel.getApartamento().getPredio()));
        LoteResponseDto lote = imovel.getLote() == null ? null : LoteMapper.toResponse(imovel.getLote());
        CorretorVitrineResponseDto corretor = toCorretorResponse(imovel.getCorretor());

        return new ImovelVitrineResponseDto(
                imovel.getId(), imovel.getTitulo(), imovel.getFinalidade(), imovel.getNatureza(),
                casa, terreno, apartamento, lote, imovel.getValorVenda(), imovel.getValorAluguel(),
                imovel.getValorCondominio(), imovel.getValorIptu(), imovel.getDescricao(), enderecoResponse,
                corretor, caracteristicas, fotos);
    }

    private void validarFiltro(ImovelVitrineFiltro filtro) {
        if (filtro == null || filtro.cidadeId() == null || filtro.cidadeId() < 1) {
            throw new ImovelInvalidoException("Informe uma cidade válida para buscar imóveis.");
        }
        if (filtro.pagina() == null || filtro.pagina() < 0
                || filtro.tamanho() == null || filtro.tamanho() < 1 || filtro.tamanho() > 50) {
            throw new ImovelInvalidoException("A paginação deve usar página a partir de 0 e tamanho entre 1 e 50.");
        }
        validarFaixa("preço", filtro.precoMinimo(), filtro.precoMaximo());
        validarFaixa("área", filtro.areaMinima(), filtro.areaMaxima());
        validarNaoNegativo("quartos", filtro.quartos());
        validarNaoNegativo("suítes", filtro.suites());
        validarNaoNegativo("vagas", filtro.vagas());
        if (filtro.caracteristicas() != null
                && filtro.caracteristicas().stream().anyMatch(id -> id == null || id < 1)) {
            throw new ImovelInvalidoException("Informe IDs válidos para as características.");
        }
    }

    private void validarFaixa(String campo, BigDecimal minimo, BigDecimal maximo) {
        if ((minimo != null && minimo.signum() < 0) || (maximo != null && maximo.signum() < 0)
                || (minimo != null && maximo != null && minimo.compareTo(maximo) > 0)) {
            throw new ImovelInvalidoException("A faixa de " + campo + " informada é inválida.");
        }
    }

    private void validarNaoNegativo(String campo, Integer valor) {
        if (valor != null && valor < 0) {
            throw new ImovelInvalidoException("O filtro de " + campo + " não pode ser negativo.");
        }
    }

    private ImovelVitrineFiltro normalizar(ImovelVitrineFiltro filtro) {
        String texto = filtro.texto() == null ? null : filtro.texto().trim();
        String bairro = filtro.bairro() == null ? null : filtro.bairro().trim();
        if (texto != null && texto.isEmpty()) texto = null;
        if (bairro != null && bairro.isEmpty()) bairro = null;
        List<Integer> caracteristicas = filtro.caracteristicas() == null
                ? null : filtro.caracteristicas().stream().filter(Objects::nonNull).distinct().toList();
        return new ImovelVitrineFiltro(
                filtro.cidadeId(), texto, filtro.finalidade(), filtro.natureza(),
                filtro.precoMinimo(), filtro.precoMaximo(), filtro.quartos(), filtro.suites(), filtro.vagas(),
                bairro, filtro.areaMinima(), filtro.areaMaxima(), caracteristicas,
                filtro.ordenacao() == null ? OrdenacaoVitrine.MAIS_RECENTES : filtro.ordenacao(),
                filtro.pagina(), filtro.tamanho());
    }

    private CidadeResponseDto toCidadeResponse(Cidade cidade) {
        return new CidadeResponseDto(cidade.getId(), cidade.getNome(), cidade.getUf().name());
    }

    private EnderecoImovelResponseDto toEnderecoImovelResponse(Predio predio) {
        return new EnderecoImovelResponseDto(null, predio.getCep(), predio.getLogradouro(), predio.getNumero(),
                predio.getComplemento(), predio.getBairro(), predio.getCidade().getId(), predio.getCidade().getNome(),
                predio.getCidade().getUf().name(), predio.getLatitude(), predio.getLongitude());
    }

    private PredioVitrineResponseDto toPredioResponse(Predio predio) {
        EnderecoPredioResponseDto endereco = new EnderecoPredioResponseDto(
                predio.getCep(), predio.getLogradouro(), predio.getNumero(), predio.getComplemento(),
                predio.getBairro(), predio.getCidade().getId(), predio.getCidade().getNome(),
                predio.getCidade().getUf().name(), predio.getLatitude(), predio.getLongitude());
        return new PredioVitrineResponseDto(predio.getNome(), endereco, predio.getNumeroAndares(),
                predio.getUnidadesPorAndar(), predio.getAnoConstrucao(), predio.getElevadores(),
                predio.getPortaria24h(), predio.getPiscina(), predio.getAcademia(), predio.getSalaoFestas(),
                predio.getPlayground(), predio.getValorCondominioBase());
    }

    private CorretorVitrineResponseDto toCorretorResponse(Corretor corretor) {
        return new CorretorVitrineResponseDto(corretor.getId(), corretor.getUsuario().getNome(),
                corretor.getCreci(), corretor.getFotoUrl(),
                ImagemUrl.extrairImagemId(corretor.getFotoUrl(), TipoImagem.CORRETOR, corretor.getId()),
                corretor.getTelefone(), corretor.getWhatsapp(), corretor.getUsuario().getEmail(),
                corretor.getApresentacao());
    }
}
