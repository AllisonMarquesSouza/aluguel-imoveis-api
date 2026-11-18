package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imovel.DocumentoImovelResponseDto;
import com.br.meuimovel.enums.TipoDocumentoImovel;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.DocumentoImovelAlreadyExistsException;
import com.br.meuimovel.exception.DocumentoImovelInvalidoException;
import com.br.meuimovel.exception.DocumentoImovelNotFoundException;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.DocumentoImovel;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.DocumentoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.storage.DocumentoStorageService;
import com.br.meuimovel.storage.StoredDocument;
import com.br.meuimovel.validator.DocumentoImovelValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoImovelService {
    private final ImovelRepository imovelRepository;
    private final DocumentoImovelRepository documentoRepository;
    private final DocumentoStorageService documentoStorageService;
    private final DocumentoImovelValidator documentoValidator;

    @Transactional
    public DocumentoImovelResponseDto adicionar(
            Integer imovelId, TipoDocumentoImovel tipo, LocalDate dataDocumento, MultipartFile arquivo) {
        Usuario gestor = getGestorAutenticado();
        Imovel imovel = buscarImovel(imovelId, gestor.getEmpresa());
        validarDadosDocumento(tipo, dataDocumento);
        documentoValidator.validar(arquivo);
        if (documentoRepository.existsByImovel_IdAndTipo(imovelId, tipo)) {
            throw new DocumentoImovelAlreadyExistsException(
                    "Já existe um documento desse tipo associado ao imóvel; use PUT para substituí-lo.");
        }

        String arquivoId = documentoStorageService.salvar(arquivo, imovelId);
        try {
            DocumentoImovel documento = new DocumentoImovel();
            documento.setImovel(imovel);
            documento.setTipo(tipo);
            documento.setNomeArquivo(documentoValidator.nomeArquivo(arquivo));
            documento.setArquivoId(arquivoId);
            documento.setDataDocumento(dataDocumento);
            documento.setCriadoEm(LocalDateTime.now());
            return toResponse(documentoRepository.save(documento));
        } catch (RuntimeException exception) {
            documentoStorageService.deletar(imovelId, arquivoId);
            throw exception;
        }
    }

    @Transactional
    public DocumentoImovelResponseDto atualizar(
            Integer imovelId, Integer documentoId, LocalDate dataDocumento, MultipartFile arquivo) {
        Usuario gestor = getGestorAutenticado();
        buscarImovel(imovelId, gestor.getEmpresa());
        validarDataDocumento(dataDocumento);
        documentoValidator.validar(arquivo);
        DocumentoImovel documento = buscarDocumento(imovelId, documentoId);

        documentoStorageService.substituir(imovelId, documento.getArquivoId(), arquivo);
        documento.setNomeArquivo(documentoValidator.nomeArquivo(arquivo));
        documento.setDataDocumento(dataDocumento);
        return toResponse(documentoRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public List<DocumentoImovelResponseDto> listar(Integer imovelId) {
        Usuario gestor = getGestorAutenticado();
        buscarImovel(imovelId, gestor.getEmpresa());
        return documentoRepository.findAllByImovel_IdOrderByTipoAsc(imovelId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ArquivoDocumento buscarArquivo(Integer imovelId, Integer documentoId) {
        Usuario gestor = getGestorAutenticado();
        buscarImovel(imovelId, gestor.getEmpresa());
        DocumentoImovel documento = buscarDocumento(imovelId, documentoId);
        StoredDocument arquivo = documentoStorageService.carregar(imovelId, documento.getArquivoId());
        return new ArquivoDocumento(documento.getNomeArquivo(), arquivo.resource(), arquivo.contentType());
    }

    private Imovel buscarImovel(Integer imovelId, Empresa empresa) {
        return imovelRepository.findByIdAndEmpresa_Id(imovelId, empresa.getId())
                .orElseThrow(() -> new ImovelNotFoundException("Imóvel não encontrado nesta empresa."));
    }

    private DocumentoImovel buscarDocumento(Integer imovelId, Integer documentoId) {
        return documentoRepository.findByIdAndImovel_Id(documentoId, imovelId)
                .orElseThrow(() -> new DocumentoImovelNotFoundException("Documento não encontrado neste imóvel."));
    }

    private void validarDadosDocumento(TipoDocumentoImovel tipo, LocalDate dataDocumento) {
        if (tipo == null) {
            throw new DocumentoImovelInvalidoException("Informe o tipo do documento.");
        }
        validarDataDocumento(dataDocumento);
    }

    private void validarDataDocumento(LocalDate dataDocumento) {
        if (dataDocumento == null) {
            throw new DocumentoImovelInvalidoException("Informe a data do documento.");
        }
    }

    private DocumentoImovelResponseDto toResponse(DocumentoImovel documento) {
        return new DocumentoImovelResponseDto(
                documento.getId(), documento.getTipo(), documento.getNomeArquivo(), documento.getDataDocumento(),
                documento.getCriadoEm(), "/imovel/" + documento.getImovel().getId()
                + "/documentos/" + documento.getId() + "/arquivo");
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
            throw new EmpresaInativaException("A empresa deve estar ativa para gerenciar documentos de imóveis.");
        }
        return usuario;
    }

    public record ArquivoDocumento(String nomeArquivo, org.springframework.core.io.Resource resource,
                                   String contentType) {}
}
