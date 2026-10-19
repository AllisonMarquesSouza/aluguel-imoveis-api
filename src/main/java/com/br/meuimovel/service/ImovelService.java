package com.br.meuimovel.service;

import com.br.meuimovel.dtos.caracteristica.CaracteristicaResponseDto;
import com.br.meuimovel.dtos.imovel.*;
import com.br.meuimovel.enums.FinalidadeImovel;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.*;
import com.br.meuimovel.model.*;
import com.br.meuimovel.repository.*;
import com.br.meuimovel.validator.FotoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImovelService {
    private final ImovelRepository imovelRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final CorretorRepository corretorRepository;
    private final CidadeRepository cidadeRepository;
    private final CaracteristicaRepository caracteristicaRepository;
    private final EmpresaRepository empresaRepository;
    private final FotoImovelRepository fotoImovelRepository;
    private final FileStorageService fileStorageService;
    private final FotoValidator fotoValidator;

    @Transactional
    public ImovelResponseDto create(ImovelCreateDto createDto, List<MultipartFile> fotos) {
        Usuario usuario = getCorretorAutenticado();
        Empresa empresa = usuario.getEmpresa();
        if (!empresa.isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para cadastrar imóveis.");
        }

        validarPrecos(createDto);
        validarFotos(fotos, createDto.fotoCapaOrdem());

        Integer empresaId = empresa.getId();
        String codigo = createDto.codigo().trim();
        if (imovelRepository.existsByEmpresa_IdAndCodigo(empresaId, codigo)) {
            throw new ImovelAlreadyExistsException("Já existe um imóvel com esse código na empresa.");
        }

        Corretor corretor = corretorRepository.findById(usuario.getId())
                .orElseThrow(() -> new UnauthorizedOperationException(
                        "O usuário autenticado não possui cadastro de corretor."));
        Proprietario proprietario = proprietarioRepository
                .findByIdAndEmpresa_Id(createDto.proprietarioId(), empresaId)
                .orElseThrow(() -> new ProprietarioNotFoundException(
                        "Proprietário não encontrado nesta empresa."));
        Cidade cidade = cidadeRepository.findById(createDto.endereco().cidadeId())
                .orElseThrow(() -> new CidadeNotFoundException("Cidade do imóvel não encontrada."));

        if (!empresaRepository.atuaNaCidade(empresaId, cidade.getId())) {
            throw new ImovelInvalidoException("A cidade não pertence à área de atuação da empresa.");
        }

        Set<Caracteristica> caracteristicas = carregarCaracteristicas(createDto.caracteristicasIds());
        Imovel imovel = criarImovel(createDto, empresa, corretor, proprietario, cidade, caracteristicas, codigo);
        imovel = imovelRepository.save(imovel);

        List<String> arquivosSalvos = new ArrayList<>();
        try {
            for (int index = 0; index < fotos.size(); index++) {
                int ordem = index + 1;
                String url = fileStorageService.salvarFotoImovel(fotos.get(index), imovel.getId(), ordem);
                arquivosSalvos.add(url);

                FotoImovel foto = new FotoImovel();
                foto.setImovel(imovel);
                foto.setUrl(url);
                foto.setOrdem(ordem);
                foto.setCapa(ordem == createDto.fotoCapaOrdem());
                fotoImovelRepository.save(foto);
            }
            return toResponse(imovel);
        } catch (RuntimeException exception) {
            arquivosSalvos.forEach(this::tentarExcluirArquivo);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ImovelResponseDto> listByEmpresa() {
        Integer empresaId = getEmpresaIdDoCorretorAutenticado();
        return imovelRepository.findAllByEmpresa_Id(empresaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public ImovelResponseDto getById(Integer id) {
        Integer empresaId = getEmpresaIdDoCorretorAutenticado();
        Imovel imovel = imovelRepository.findByIdAndEmpresa_Id(id, empresaId)
                .orElseThrow(() -> new ImovelNotFoundException("Imóvel não encontrado nesta empresa."));
        return toResponse(imovel);
    }

    private Integer getEmpresaIdDoCorretorAutenticado() {
        return getCorretorAutenticado().getEmpresa().getId();
    }

    private Usuario getCorretorAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario usuario)
                || usuario.getPerfil() != UsuarioPerfil.CORRETOR
                || usuario.getStatus() != UsuarioStatus.ATIVO
                || usuario.getEmpresa() == null ) {
            throw new UnauthorizedOperationException("A operação exige um corretor ativo vinculado a uma empresa.");
        }
        return usuario;
    }

    private void validarPrecos(ImovelCreateDto dto) {
        boolean exigeVenda = dto.finalidade() == FinalidadeImovel.VENDA
                || dto.finalidade() == FinalidadeImovel.VENDA_E_ALUGUEL;
        boolean exigeAluguel = dto.finalidade() == FinalidadeImovel.ALUGUEL
                || dto.finalidade() == FinalidadeImovel.VENDA_E_ALUGUEL;

        if (exigeVenda && dto.valorVenda() == null) {
            throw new ImovelInvalidoException("Informe o preço de venda para a finalidade selecionada.");
        }
        if (exigeAluguel && dto.valorAluguel() == null) {
            throw new ImovelInvalidoException("Informe o preço de aluguel para a finalidade selecionada.");
        }
    }

    private void validarFotos(List<MultipartFile> fotos, Integer fotoCapaOrdem) {
        if (fotos == null || fotos.isEmpty()) {
            throw new FotoInvalidaException("Envie ao menos uma foto do imóvel.");
        }
        if (fotoCapaOrdem == null || fotoCapaOrdem < 1 || fotoCapaOrdem > fotos.size()) {
            throw new ImovelInvalidoException("A ordem da foto de capa deve corresponder a uma foto enviada.");
        }
        fotos.forEach(fotoValidator::validar); //validando cada foto individualmente.
    }

    private Set<Caracteristica> carregarCaracteristicas(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Caracteristica> encontradas = caracteristicaRepository.findAllById(ids);
        if (encontradas.size() != ids.size()) {
            throw new CaracteristicaNotFoundException("Uma ou mais características não foram encontradas.");
        }
        return new HashSet<>(encontradas);
    }

    private Imovel criarImovel(ImovelCreateDto dto, Empresa empresa, Corretor corretor,
                               Proprietario proprietario, Cidade cidade,
                               Set<Caracteristica> caracteristicas, String codigo) {
        Imovel imovel = Imovel.builder()
                .codigo(codigo)
                .titulo(dto.titulo().trim())
                .empresa(empresa)
                .corretor(corretor)
                .proprietario(proprietario)
                .finalidade(dto.finalidade())
                .valorVenda(dto.valorVenda())
                .valorAluguel(dto.valorAluguel())
                .valorCondominio(dto.valorCondominio())
                .valorIptu(dto.valorIptu())
                .descricao(dto.descricao())
                .caracteristicas(caracteristicas)
                .build();

        EnderecoImovel endereco = EnderecoImovel.builder()
                .imovel(imovel)
                .cep(dto.endereco().cep())
                .logradouro(dto.endereco().logradouro().trim())
                .numero(dto.endereco().numero().trim())
                .complemento(dto.endereco().complemento())
                .bairro(dto.endereco().bairro().trim())
                .cidade(cidade)
                .latitude(dto.endereco().latitude())
                .longitude(dto.endereco().longitude())
                .build();
        imovel.setEndereco(endereco);
        return imovel;
    }

    private ImovelResponseDto toResponse(Imovel imovel) {
        EnderecoImovel endereco = imovel.getEndereco();
        EnderecoImovelResponseDto enderecoResponse = new EnderecoImovelResponseDto(
                endereco.getId(), endereco.getCep(), endereco.getLogradouro(), endereco.getNumero(),
                endereco.getComplemento(), endereco.getBairro(), endereco.getCidade().getId(),
                endereco.getCidade().getNome(), endereco.getCidade().getUf().name(),
                endereco.getLatitude(), endereco.getLongitude());

        Set<CaracteristicaResponseDto> caracteristicas = imovel.getCaracteristicas().stream()
                .map(caracteristica -> new CaracteristicaResponseDto(caracteristica.getId(), caracteristica.getNome()))
                .collect(java.util.stream.Collectors.toSet());
        List<FotoImovelResponseDto> fotos = fotoImovelRepository
                .findAllByImovel_IdOrderByOrdemAsc(imovel.getId())
                .stream()
                .map(foto -> new FotoImovelResponseDto(foto.getId(), foto.getUrl(), foto.getOrdem(), foto.isCapa()))
                .toList();

        return new ImovelResponseDto(
                imovel.getId(), imovel.getCodigo(), imovel.getTitulo(), imovel.getEmpresa().getId(),
                imovel.getCorretor().getId(), imovel.getCorretor().getUsuario().getNome(),
                imovel.getProprietario().getId(), imovel.getProprietario().getNome(), imovel.getFinalidade(),
                imovel.getValorVenda(), imovel.getValorAluguel(), imovel.getValorCondominio(),
                imovel.getValorIptu(), imovel.getDescricao(), enderecoResponse, caracteristicas, fotos);
    }

    private void tentarExcluirArquivo(String caminho) {
        try {
            fileStorageService.deletar(caminho);
        } catch (RuntimeException ignored) {
            // Preserva a falha original do cadastro.
        }
    }
}
