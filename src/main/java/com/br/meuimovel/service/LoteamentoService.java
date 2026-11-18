package com.br.meuimovel.service;

import com.br.meuimovel.dtos.loteamento.LoteamentoCreateDto;
import com.br.meuimovel.dtos.loteamento.LoteamentoResponseDto;
import com.br.meuimovel.dtos.loteamento.QuadraCreateDto;
import com.br.meuimovel.dtos.loteamento.QuadraResponseDto;
import com.br.meuimovel.enums.SituacaoLote;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CidadeNotFoundException;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.LoteamentoNotFoundException;
import com.br.meuimovel.exception.QuadraAlreadyExistsException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.mapper.LoteamentoMapper;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Loteamento;
import com.br.meuimovel.model.Quadra;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CidadeRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.LoteRepository;
import com.br.meuimovel.repository.LoteamentoRepository;
import com.br.meuimovel.repository.QuadraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoteamentoService {
    private final LoteamentoRepository loteamentoRepository;
    private final QuadraRepository quadraRepository;
    private final LoteRepository loteRepository;
    private final CidadeRepository cidadeRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public LoteamentoResponseDto create(LoteamentoCreateDto createDto) {
        Empresa empresa = getEmpresaDoCorretorAutenticado();
        validarEmpresaAtiva(empresa);
        Cidade cidade = carregarCidadeDaEmpresa(createDto.cidadeId(), empresa);

        Loteamento loteamento = new Loteamento();
        loteamento.setEmpresa(empresa);
        aplicarDados(loteamento, createDto, cidade);
        return toResponse(loteamentoRepository.save(loteamento));
    }

    @Transactional(readOnly = true)
    public List<LoteamentoResponseDto> listByEmpresa() {
        Integer empresaId = getEmpresaDoCorretorAutenticado().getId();
        return loteamentoRepository.findAllByEmpresa_IdOrderByNomeAsc(empresaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoteamentoResponseDto getById(Integer id) {
        Integer empresaId = getEmpresaDoCorretorAutenticado().getId();
        return toResponse(buscarLoteamento(id, empresaId));
    }

    @Transactional
    public QuadraResponseDto createQuadra(Integer loteamentoId, QuadraCreateDto createDto) {
        Empresa empresa = getEmpresaDoCorretorAutenticado();
        validarEmpresaAtiva(empresa);
        Loteamento loteamento = buscarLoteamento(loteamentoId, empresa.getId());
        String identificacao = normalizarIdentificacao(createDto.identificacao());
        validarIdentificacaoDisponivel(loteamentoId, identificacao);

        Quadra quadra = quadraRepository.save(Quadra.builder()
                .loteamento(loteamento)
                .identificacao(identificacao)
                .build());
        return toQuadraResponse(quadra);
    }

    private Loteamento buscarLoteamento(Integer id, Integer empresaId) {
        return loteamentoRepository.findByIdAndEmpresa_Id(id, empresaId)
                .orElseThrow(() -> new LoteamentoNotFoundException("Loteamento não encontrado nesta empresa."));
    }

    private void aplicarDados(Loteamento loteamento, LoteamentoCreateDto dto, Cidade cidade) {
        loteamento.setNome(dto.nome().trim());
        loteamento.setIncorporadora(dto.incorporadora().trim());
        loteamento.setCidade(cidade);
        loteamento.setAsfalto(dto.asfalto());
        loteamento.setAgua(dto.agua());
        loteamento.setEnergia(dto.energia());
        loteamento.setEsgoto(dto.esgoto());
        loteamento.setIluminacaoPublica(dto.iluminacaoPublica());
        loteamento.setAreasLazer(normalizarTextoOpcional(dto.areasLazer()));
    }

    private Cidade carregarCidadeDaEmpresa(Integer cidadeId, Empresa empresa) {
        Cidade cidade = cidadeRepository.findById(cidadeId)
                .orElseThrow(() -> new CidadeNotFoundException("Cidade do loteamento não encontrada."));
        if (!empresaRepository.atuaNaCidade(empresa.getId(), cidade.getId())) {
            throw new ImovelInvalidoException("A cidade do loteamento não pertence à área de atuação da empresa.");
        }
        return cidade;
    }

    private LoteamentoResponseDto toResponse(Loteamento loteamento) {
        List<QuadraResponseDto> quadras = quadraRepository
                .findAllByLoteamento_IdOrderByIdentificacaoAsc(loteamento.getId())
                .stream()
                .map(this::toQuadraResponse)
                .toList();
        return LoteamentoMapper.toResponse(loteamento, quadras);
    }

    private QuadraResponseDto toQuadraResponse(Quadra quadra) {
        return new QuadraResponseDto(
                quadra.getId(), quadra.getIdentificacao(),
                loteRepository.countByQuadra_IdAndSituacao(quadra.getId(), SituacaoLote.DISPONIVEL),
                loteRepository.countByQuadra_IdAndSituacao(quadra.getId(), SituacaoLote.VENDIDO));
    }

    private void validarIdentificacaoDisponivel(Integer loteamentoId, String identificacao) {
        if (quadraRepository.existsByLoteamento_IdAndIdentificacaoIgnoreCase(loteamentoId, identificacao)) {
            throw new QuadraAlreadyExistsException("Já existe uma quadra com essa identificação no loteamento.");
        }
    }

    private String normalizarIdentificacao(String identificacao) {
        return identificacao.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Empresa getEmpresaDoCorretorAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario usuario)
                || usuario.getPerfil() != UsuarioPerfil.CORRETOR
                || usuario.getStatus() != UsuarioStatus.ATIVO
                || usuario.getEmpresa() == null) {
            throw new UnauthorizedOperationException("A operação exige um corretor ativo vinculado a uma empresa.");
        }
        return usuario.getEmpresa();
    }

    private void validarEmpresaAtiva(Empresa empresa) {
        if (!empresa.isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para cadastrar loteamentos e quadras.");
        }
    }
}
