package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imovel.LoteResponseDto;
import com.br.meuimovel.dtos.loteamento.LoteSituacaoUpdateDto;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.LoteNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.mapper.LoteMapper;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.HistoricoSituacaoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Lote;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.HistoricoSituacaoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoteService {
    private final LoteRepository loteRepository;
    private final ImovelRepository imovelRepository;
    private final HistoricoSituacaoImovelRepository historicoRepository;

    @Transactional
    public LoteResponseDto updateSituacao(Integer id, LoteSituacaoUpdateDto updateDto) {
        Usuario usuario = getGestorAutenticado();
        Empresa empresa = usuario.getEmpresa();
        if (!empresa.isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para atualizar a situação dos lotes.");
        }

        Lote lote = loteRepository.findByIdAndImovel_Empresa_Id(id, empresa.getId())
                .orElseThrow(() -> new LoteNotFoundException("Lote não encontrado nesta empresa."));
        Imovel imovel = lote.getImovel();
        boolean situacaoImovelMudou = imovel.getSituacao() != updateDto.situacao();
        boolean situacaoLoteMudou = lote.getSituacao() != updateDto.situacao();
        if (situacaoImovelMudou || situacaoLoteMudou) {
            if (updateDto.observacao() == null || updateDto.observacao().isBlank()) {
                throw new ImovelInvalidoException("Informe uma observação para alterar a situação do lote.");
            }
            if (situacaoImovelMudou) {
                HistoricoSituacaoImovel historico = new HistoricoSituacaoImovel();
                historico.setImovel(imovel);
                historico.setUsuario(usuario);
                historico.setSituacaoAnterior(imovel.getSituacao());
                historico.setSituacaoNova(updateDto.situacao());
                historico.setObservacao(updateDto.observacao().trim());
                historico.setAlteradoEm(java.time.LocalDateTime.now());
                historicoRepository.save(historico);
                imovel.setSituacao(updateDto.situacao());
                imovelRepository.save(imovel);
            }
            lote.setSituacao(updateDto.situacao());
        }
        return LoteMapper.toResponse(loteRepository.save(lote));
    }

    private Usuario getGestorAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario usuario)
                || usuario.getPerfil() != UsuarioPerfil.GESTOR
                || usuario.getStatus() != UsuarioStatus.ATIVO
                || usuario.getEmpresa() == null) {
            throw new UnauthorizedOperationException("A operação exige um gestor ativo vinculado a uma empresa.");
        }
        return usuario;
    }
}
