package com.br.meuimovel.service;

import com.br.meuimovel.dtos.proprietario.ProprietarioCreateDto;
import com.br.meuimovel.dtos.proprietario.ProprietarioResponseDto;
import com.br.meuimovel.dtos.proprietario.ProprietarioUpdateDto;
import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ProprietarioAlreadyExistsException;
import com.br.meuimovel.exception.ProprietarioNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Proprietario;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.ProprietarioRepository;
import com.br.meuimovel.validator.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProprietarioService {
    private final ProprietarioRepository proprietarioRepository;
    private final DocumentoValidator documentoValidator;


    private Usuario getCorretorAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario corretor)
                || corretor.getPerfil() != UsuarioPerfil.CORRETOR
                || corretor.getStatus() != UsuarioStatus.ATIVO
                || corretor.getEmpresa() == null) {
            throw new UnauthorizedOperationException("Para processar essa operacao" +
                    " precisa ser um corretor e estar ativo!");
        }
        return corretor;
    }

    public ProprietarioResponseDto getByDocumento(TipoDocumento tipoDocumento, String documento){
        String documentoValid = documentoValidator.validar(tipoDocumento, documento);
        Integer empresaId = getCorretorAutenticado().getEmpresa().getId();
        Proprietario proprietario = proprietarioRepository.findByEmpresaIdAndDocumento(empresaId, documentoValid)
                .orElseThrow(() -> new ProprietarioNotFoundException(
                                "Proprietarico com documento : "+ documento +" nao encontrado!"));
        return toResponse(proprietario);
    }

    public List<ProprietarioResponseDto> listByNome(String nome){
        Integer empresaId = getCorretorAutenticado().getEmpresa().getId();
        return proprietarioRepository.findByEmpresaIdAndNomeContainingIgnoreCase(empresaId, nome)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProprietarioResponseDto create(ProprietarioCreateDto createDto){
        Usuario corretor = getCorretorAutenticado();
        Empresa empresa = corretor.getEmpresa();

        String documento =
                documentoValidator
                .validar(createDto.tipoDocumento(), createDto.documento());
        if(proprietarioRepository.existsByEmpresaIdAndDocumento(empresa.getId(), documento)){
            throw new ProprietarioAlreadyExistsException("Proprietario com esse documento já existe na empresa!");
        }

        Proprietario proprietario = Proprietario.builder()
                .empresa(empresa)
                .tipoDocumento(createDto.tipoDocumento())
                .documento(documento)
                .nome(createDto.nome())
                .telefone(createDto.telefone())
                .email(createDto.email())
                .observacoes(createDto.observacoes())
                .build();

        return toResponse(proprietarioRepository.save(proprietario));
    }

    @Transactional
    public ProprietarioResponseDto atualizar(Integer id, ProprietarioUpdateDto dto) {
        Integer empresaId = getCorretorAutenticado().getEmpresa().getId();
        Proprietario proprietario = proprietarioRepository.findByIdAndEmpresa_Id(id, empresaId)
                .orElseThrow(() -> new ProprietarioNotFoundException("Proprietário não encontrado nesta empresa."));
        String documento = documentoValidator.validar(dto.tipoDocumento(), dto.documento());
        if (proprietarioRepository.existsByEmpresaIdAndDocumentoAndIdNot(empresaId, documento, id)) {
            throw new ProprietarioAlreadyExistsException("Proprietário com esse documento já existe na empresa.");
        }

        proprietario.setTipoDocumento(dto.tipoDocumento());
        proprietario.setDocumento(documento);
        proprietario.setNome(dto.nome().trim());
        proprietario.setTelefone(dto.telefone().trim());
        proprietario.setEmail(dto.email().trim());
        proprietario.setObservacoes(dto.observacoes() == null || dto.observacoes().isBlank()
                ? null : dto.observacoes().trim());
        return toResponse(proprietarioRepository.save(proprietario));
    }

    private ProprietarioResponseDto toResponse(Proprietario proprietario) {
        return new ProprietarioResponseDto(
                proprietario.getId(), proprietario.getTipoDocumento(), proprietario.getDocumento(),
                proprietario.getNome(), proprietario.getTelefone(), proprietario.getEmail(),
                proprietario.getObservacoes()
        );
    }

}
