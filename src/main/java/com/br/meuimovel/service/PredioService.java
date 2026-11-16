package com.br.meuimovel.service;

import com.br.meuimovel.dtos.predio.EnderecoPredioDto;
import com.br.meuimovel.dtos.predio.PredioCreateDto;
import com.br.meuimovel.dtos.predio.PredioResponseDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CidadeNotFoundException;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.PredioNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.mapper.PredioMapper;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Predio;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CidadeRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.PredioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PredioService {
    private final PredioRepository predioRepository;
    private final CidadeRepository cidadeRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public PredioResponseDto create(PredioCreateDto createDto) {
        Usuario usuario = getCorretorAutenticado();
        Empresa empresa = usuario.getEmpresa();
        validarEmpresaAtiva(empresa);
        Cidade cidade = carregarCidadeDaEmpresa(createDto.endereco(), empresa);

        Predio predio = Predio.builder()
                .empresa(empresa)
                .build();
        aplicarDados(predio, createDto, cidade);
        return PredioMapper.toResponse(predioRepository.save(predio));
    }

    @Transactional(readOnly = true)
    public List<PredioResponseDto> listByEmpresa() {
        Integer empresaId = getCorretorAutenticado().getEmpresa().getId();
        return predioRepository.findAllByEmpresa_IdOrderByNomeAsc(empresaId)
                .stream()
                .map(PredioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PredioResponseDto getById(Integer id) {
        Integer empresaId = getCorretorAutenticado().getEmpresa().getId();
        return PredioMapper.toResponse(predioRepository.findByIdAndEmpresa_Id(id, empresaId)
                .orElseThrow(() -> new PredioNotFoundException("Prédio não encontrado nesta empresa.")));
    }

    @Transactional
    public PredioResponseDto update(Integer id, PredioCreateDto updateDto) {
        Usuario usuario = getCorretorAutenticado();
        Empresa empresa = usuario.getEmpresa();
        validarEmpresaAtiva(empresa);
        Predio predio = predioRepository.findByIdAndEmpresa_Id(id, empresa.getId())
                .orElseThrow(() -> new PredioNotFoundException("Prédio não encontrado nesta empresa."));
        Cidade cidade = carregarCidadeDaEmpresa(updateDto.endereco(), empresa);

        aplicarDados(predio, updateDto, cidade);
        return PredioMapper.toResponse(predioRepository.save(predio));
    }

    private void aplicarDados(Predio predio, PredioCreateDto dto, Cidade cidade) {
        EnderecoPredioDto enderecoDto = dto.endereco();
        predio.setNome(dto.nome().trim());
        predio.setCep(enderecoDto.cep());
        predio.setLogradouro(enderecoDto.logradouro().trim());
        predio.setNumero(enderecoDto.numero().trim());
        predio.setComplemento(enderecoDto.complemento());
        predio.setBairro(enderecoDto.bairro().trim());
        predio.setCidade(cidade);
        predio.setLatitude(enderecoDto.latitude());
        predio.setLongitude(enderecoDto.longitude());
        predio.setNumeroAndares(dto.numeroAndares());
        predio.setUnidadesPorAndar(dto.unidadesPorAndar());
        predio.setAnoConstrucao(dto.anoConstrucao());
        predio.setElevadores(dto.elevadores());
        predio.setPortaria24h(dto.portaria24h());
        predio.setPiscina(dto.piscina());
        predio.setAcademia(dto.academia());
        predio.setSalaoFestas(dto.salaoFestas());
        predio.setPlayground(dto.playground());
        predio.setValorCondominioBase(dto.valorCondominioBase());
    }

    private Cidade carregarCidadeDaEmpresa(EnderecoPredioDto endereco, Empresa empresa) {
        Cidade cidade = cidadeRepository.findById(endereco.cidadeId())
                .orElseThrow(() -> new CidadeNotFoundException("Cidade do prédio não encontrada."));
        if (!empresaRepository.atuaNaCidade(empresa.getId(), cidade.getId())) {
            throw new ImovelInvalidoException("A cidade não pertence à área de atuação da empresa.");
        }
        return cidade;
    }

    private void validarEmpresaAtiva(Empresa empresa) {
        if (!empresa.isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para cadastrar ou atualizar prédios.");
        }
    }

    private Usuario getCorretorAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario usuario)
                || usuario.getPerfil() != UsuarioPerfil.CORRETOR
                || usuario.getStatus() != UsuarioStatus.ATIVO
                || usuario.getEmpresa() == null) {
            throw new UnauthorizedOperationException("A operação exige um corretor ativo vinculado a uma empresa.");
        }
        return usuario;
    }
}
