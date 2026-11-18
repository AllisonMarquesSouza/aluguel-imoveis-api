package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imovel.HistoricoSituacaoImovelResponseDto;
import com.br.meuimovel.dtos.imovel.ImovelGestaoResponseDto;
import com.br.meuimovel.dtos.imovel.PublicacaoImovelUpdateDto;
import com.br.meuimovel.dtos.imovel.SituacaoImovelUpdateDto;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.EnderecoImovel;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.HistoricoSituacaoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Lote;
import com.br.meuimovel.model.Predio;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.HistoricoSituacaoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImovelGestaoService {
    private final ImovelRepository imovelRepository;
    private final HistoricoSituacaoImovelRepository historicoRepository;
    private final LoteRepository loteRepository;

    @Transactional
    public ImovelGestaoResponseDto atualizarPublicacao(Integer imovelId, PublicacaoImovelUpdateDto dto) {
        Usuario gestor = getGestorAutenticado();
        Imovel imovel = buscarImovel(imovelId, gestor.getEmpresa());
        if (dto.publicado()) {
            validarEnderecoCompleto(imovel);
        }
        imovel.setPublicado(dto.publicado());
        return toResponse(imovelRepository.save(imovel));
    }

    @Transactional
    public ImovelGestaoResponseDto atualizarSituacao(Integer imovelId, SituacaoImovelUpdateDto dto) {
        Usuario gestor = getGestorAutenticado();
        Imovel imovel = buscarImovel(imovelId, gestor.getEmpresa());
        SituacaoImovel situacaoAnterior = imovel.getSituacao();
        if (situacaoAnterior != dto.situacao()) {
            String observacao = normalizarObservacao(dto.observacao());
            if (observacao == null) {
                throw new ImovelInvalidoException("Informe uma observação para alterar a situação do imóvel.");
            }
            imovel.setSituacao(dto.situacao());
            imovelRepository.save(imovel);
            Lote lote = imovel.getLote();
            if (lote != null) {
                lote.setSituacao(dto.situacao());
                loteRepository.save(lote);
            }

            HistoricoSituacaoImovel historico = new HistoricoSituacaoImovel();
            historico.setImovel(imovel);
            historico.setUsuario(gestor);
            historico.setSituacaoAnterior(situacaoAnterior);
            historico.setSituacaoNova(dto.situacao());
            historico.setObservacao(observacao);
            historico.setAlteradoEm(LocalDateTime.now());
            historicoRepository.save(historico);
        }
        return toResponse(imovel);
    }

    @Transactional(readOnly = true)
    public List<HistoricoSituacaoImovelResponseDto> listarHistorico(Integer imovelId) {
        Usuario gestor = getGestorAutenticado();
        buscarImovel(imovelId, gestor.getEmpresa());
        return historicoRepository.findAllByImovel_IdOrderByAlteradoEmAscIdAsc(imovelId)
                .stream()
                .map(historico -> new HistoricoSituacaoImovelResponseDto(
                        historico.getId(), historico.getSituacaoAnterior(), historico.getSituacaoNova(),
                        historico.getUsuario().getId(), historico.getUsuario().getNome(),
                        historico.getAlteradoEm(), historico.getObservacao()))
                .toList();
    }

    private Imovel buscarImovel(Integer imovelId, Empresa empresa) {
        return imovelRepository.findByIdAndEmpresa_Id(imovelId, empresa.getId())
                .orElseThrow(() -> new ImovelNotFoundException("Imóvel não encontrado nesta empresa."));
    }

    private void validarEnderecoCompleto(Imovel imovel) {
        List<String> camposAusentes = new ArrayList<>();
        EnderecoImovel endereco = imovel.getEndereco();
        if (endereco != null) {
            adicionarSeAusente(camposAusentes, "CEP", endereco.getCep());
            adicionarSeAusente(camposAusentes, "logradouro", endereco.getLogradouro());
            adicionarSeAusente(camposAusentes, "número", endereco.getNumero());
            adicionarSeAusente(camposAusentes, "bairro", endereco.getBairro());
            if (endereco.getCidade() == null) camposAusentes.add("cidade");
            if (endereco.getLatitude() == null) camposAusentes.add("latitude");
            if (endereco.getLongitude() == null) camposAusentes.add("longitude");
        } else if (imovel.getApartamento() != null && imovel.getApartamento().getPredio() != null) {
            Predio predio = imovel.getApartamento().getPredio();
            adicionarSeAusente(camposAusentes, "CEP", predio.getCep());
            adicionarSeAusente(camposAusentes, "logradouro", predio.getLogradouro());
            adicionarSeAusente(camposAusentes, "número", predio.getNumero());
            adicionarSeAusente(camposAusentes, "bairro", predio.getBairro());
            if (predio.getCidade() == null) camposAusentes.add("cidade");
            if (predio.getLatitude() == null) camposAusentes.add("latitude");
            if (predio.getLongitude() == null) camposAusentes.add("longitude");
        } else {
            camposAusentes.addAll(List.of("CEP", "logradouro", "número", "bairro", "cidade", "latitude", "longitude"));
        }

        if (!camposAusentes.isEmpty()) {
            throw new ImovelInvalidoException(
                    "Não é possível publicar o imóvel. Complete o endereço: " + String.join(", ", camposAusentes) + ".");
        }
    }

    private void adicionarSeAusente(List<String> camposAusentes, String campo, String valor) {
        if (valor == null || valor.isBlank()) camposAusentes.add(campo);
    }

    private String normalizarObservacao(String observacao) {
        return observacao == null || observacao.isBlank() ? null : observacao.trim();
    }

    private ImovelGestaoResponseDto toResponse(Imovel imovel) {
        return new ImovelGestaoResponseDto(imovel.getId(), imovel.getSituacao(), imovel.isPublicado());
    }

    private Usuario getGestorAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario usuario)
                || usuario.getPerfil() != UsuarioPerfil.GESTOR
                || usuario.getStatus() != UsuarioStatus.ATIVO
                || usuario.getEmpresa() == null) {
            throw new UnauthorizedOperationException("A operação exige um gestor ativo vinculado a uma empresa.");
        }
        if (!usuario.getEmpresa().isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para gerenciar imóveis.");
        }
        return usuario;
    }
}
