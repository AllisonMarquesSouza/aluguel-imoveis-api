package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imovel.LoteResponseDto;
import com.br.meuimovel.dtos.loteamento.LoteSituacaoUpdateDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.LoteNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.mapper.LoteMapper;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Lote;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoteService {
    private final LoteRepository loteRepository;

    @Transactional
    public LoteResponseDto updateSituacao(Integer id, LoteSituacaoUpdateDto updateDto) {
        Empresa empresa = getEmpresaDoCorretorAutenticado();
        if (!empresa.isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para atualizar a situação dos lotes.");
        }

        Lote lote = loteRepository.findByIdAndImovel_Empresa_Id(id, empresa.getId())
                .orElseThrow(() -> new LoteNotFoundException("Lote não encontrado nesta empresa."));
        lote.setSituacao(updateDto.situacao());
        return LoteMapper.toResponse(loteRepository.save(lote));
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
}
